package uk.newsbias.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import uk.newsbias.dto.StoryCardDto;
import uk.newsbias.dto.StoryDetailDto;
import uk.newsbias.dto.ArticleOutDto;
import uk.newsbias.entity.Article;
import uk.newsbias.entity.Story;
import uk.newsbias.repository.ArticleRepository;
import uk.newsbias.repository.StoryRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StoryController {

    private final StoryRepository   storyRepository;
    private final ArticleRepository articleRepository;

    @GetMapping("/stories")
    public List<StoryCardDto> getStories(
            @RequestParam(defaultValue = "all") String biasFilter,
            @RequestParam(defaultValue = "30")  int limit,
            @RequestParam(defaultValue = "0")   int offset
    ) {
        PageRequest page = PageRequest.of(offset / limit, limit);
        List<Story> stories = switch (biasFilter) {
            case "left"        -> storyRepository.findByLeftCoverage(page);
            case "centre"      -> storyRepository.findByCentreCoverage(page);
            case "right"       -> storyRepository.findByRightCoverage(page);
            case "blind_spots" -> storyRepository.findBlindSpots(page);
            default            -> storyRepository.findAllByOrderByArticleCountDesc(page);
        };
        return stories.stream().map(this::toCard).toList();
    }

    @GetMapping("/stories/trending")
    public List<StoryCardDto> getTrending() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return storyRepository.findTrending(since, PageRequest.of(0, 10))
                .stream().map(this::toCard).toList();
    }

    @GetMapping("/stories/{id}")
    public StoryDetailDto getStory(@PathVariable Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Story not found"));

        List<Article> articles = articleRepository.findByStoryId(id);
        List<ArticleOutDto> articleDtos = articles.stream().map(a -> new ArticleOutDto(
                a.getId(),
                a.getOutlet().getSlug(),
                a.getOutlet().getName(),
                a.getOutlet().getBias(),
                a.getTitle(),
                a.getUrl(),
                a.getPublishedAt()
        )).toList();

        StoryCardDto card = toCard(story);
        return new StoryDetailDto(
                card.id(), card.title(), card.firstSeenAt(), card.lastSeenAt(),
                card.articleCount(), card.leftCount(), card.centreCount(), card.rightCount(),
                card.isBlindSpot(), card.blindSpotSide(), card.outletsCovered(),
                articleDtos
        );
    }

    private StoryCardDto toCard(Story s) {
        List<String> slugs = articleRepository.findOutletSlugsByStoryId(s.getId());
        return new StoryCardDto(
                s.getId(), s.getTitle(),
                s.getFirstSeenAt(), s.getLastSeenAt(),
                s.getArticleCount(), s.getLeftCount(), s.getCentreCount(), s.getRightCount(),
                s.isBlindSpot(), s.getBlindSpotSide(),
                slugs
        );
    }
}
