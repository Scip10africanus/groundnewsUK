package uk.newsbias.clustering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.newsbias.config.AppProperties;
import uk.newsbias.entity.Article;
import uk.newsbias.entity.Story;
import uk.newsbias.nlp.EmbeddingService;
import uk.newsbias.repository.ArticleRepository;
import uk.newsbias.repository.StoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusteringService {

    private final ArticleRepository articleRepository;
    private final StoryRepository   storyRepository;
    private final AppProperties     props;

    @Transactional
    public int runClustering() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(props.getRollingWindowHours());

        List<Article> unassigned = articleRepository.findUnassignedSince(cutoff);
        if (unassigned.isEmpty()) {
            log.info("No unassigned articles to cluster");
            return 0;
        }

        // Load active stories and deserialise their centroids
        List<Story>   activeStories  = new ArrayList<>(storyRepository.findActiveSince(cutoff));
        List<float[]> centroids      = activeStories.stream()
                .map(s -> s.getCentroid() != null ? EmbeddingService.fromBytes(s.getCentroid()) : null)
                .collect(Collectors.toCollection(ArrayList::new));

        int newStories = 0;

        for (Article article : unassigned) {
            float[] vec = EmbeddingService.fromBytes(article.getEmbedding());

            int    bestIdx = -1;
            double bestSim = -1.0;

            for (int i = 0; i < activeStories.size(); i++) {
                float[] centroid = centroids.get(i);
                if (centroid == null) continue;
                double sim = EmbeddingService.cosineSimilarity(vec, centroid);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestIdx = i;
                }
            }

            if (bestIdx >= 0 && bestSim >= props.getSimilarityThreshold()) {
                // Assign to existing story, update centroid (running mean)
                Story story = activeStories.get(bestIdx);
                int n = story.getArticleCount();
                float[] oldCentroid = centroids.get(bestIdx);
                float[] newCentroid = runningMean(oldCentroid, vec, n);
                centroids.set(bestIdx, newCentroid);
                story.setCentroid(EmbeddingService.toBytes(newCentroid));
                story.setLastSeenAt(LocalDateTime.now());
                article.setStory(story);
            } else {
                // Create a new story
                Story story = new Story();
                story.setTitle(article.getTitle());
                story.setFirstSeenAt(article.getPublishedAt() != null ? article.getPublishedAt() : LocalDateTime.now());
                story.setLastSeenAt(LocalDateTime.now());
                story.setCentroid(EmbeddingService.toBytes(vec));
                storyRepository.save(story);

                article.setStory(story);
                activeStories.add(story);
                centroids.add(vec);
                newStories++;
            }

            articleRepository.save(article);
        }

        // Recompute stats for all stories that received new articles
        Set<Long> affectedIds = unassigned.stream()
                .filter(a -> a.getStory() != null)
                .map(a -> a.getStory().getId())
                .collect(Collectors.toSet());

        affectedIds.forEach(this::recomputeStats);

        log.info("Clustering done: {} articles processed, {} new stories created", unassigned.size(), newStories);
        return newStories;
    }

    private void recomputeStats(Long storyId) {
        Story story = storyRepository.findById(storyId).orElse(null);
        if (story == null) return;

        List<Article> articles = articleRepository.findByStoryId(storyId);
        int left = 0, centre = 0, right = 0;
        LocalDateTime earliest = null, latest = null;

        for (Article a : articles) {
            String bucket = AppProperties.bucket(a.getOutlet().getBias());
            switch (bucket) {
                case "left"   -> left++;
                case "centre" -> centre++;
                case "right"  -> right++;
            }
            LocalDateTime pub = a.getPublishedAt() != null ? a.getPublishedAt() : a.getIngestedAt();
            if (pub != null) {
                if (earliest == null || pub.isBefore(earliest)) earliest = pub;
                if (latest   == null || pub.isAfter(latest))   latest   = pub;
            }
        }

        int bucketsWithCoverage = (left > 0 ? 1 : 0) + (centre > 0 ? 1 : 0) + (right > 0 ? 1 : 0);
        boolean blindSpot = bucketsWithCoverage == 1;
        String blindSpotSide = null;
        if (blindSpot) {
            if (left   > 0) blindSpotSide = "left";
            else if (centre > 0) blindSpotSide = "centre";
            else blindSpotSide = "right";
        }

        story.setArticleCount(articles.size());
        story.setLeftCount(left);
        story.setCentreCount(centre);
        story.setRightCount(right);
        story.setBlindSpot(blindSpot);
        story.setBlindSpotSide(blindSpotSide);
        if (earliest != null) story.setFirstSeenAt(earliest);
        if (latest   != null) story.setLastSeenAt(latest);

        storyRepository.save(story);
    }

    /** Running mean update, then L2 normalise. */
    private float[] runningMean(float[] centroid, float[] newVec, int n) {
        float[] result = new float[centroid.length];
        double norm = 0;
        for (int i = 0; i < centroid.length; i++) {
            result[i] = (centroid[i] * n + newVec[i]) / (n + 1);
            norm += (double) result[i] * result[i];
        }
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < result.length; i++) result[i] /= norm;
        return result;
    }
}
