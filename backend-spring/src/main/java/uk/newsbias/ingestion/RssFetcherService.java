package uk.newsbias.ingestion;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.newsbias.entity.Article;
import uk.newsbias.entity.Outlet;
import uk.newsbias.nlp.EmbeddingService;
import uk.newsbias.repository.ArticleRepository;
import uk.newsbias.repository.OutletRepository;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssFetcherService {

    private final OutletRepository outletRepository;
    private final ArticleRepository articleRepository;
    private final EmbeddingService embeddingService;

    public int fetchAll() {
        int total = 0;
        for (Outlet outlet : outletRepository.findAll()) {
            try {
                total += fetchOutlet(outlet);
            } catch (Exception e) {
                log.warn("[{}] Fetch failed: {}", outlet.getSlug(), e.getMessage());
            }
        }
        return total;
    }

    private int fetchOutlet(Outlet outlet) throws Exception {
        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(outlet.getRssUrl())));
        int newCount = 0;

        for (SyndEntry entry : feed.getEntries()) {
            String guid = entry.getUri() != null ? entry.getUri() : entry.getLink();
            if (guid == null || guid.isBlank()) continue;
            if (articleRepository.existsByGuid(guid)) continue;

            String title = cleanTitle(entry.getTitle());
            if (title == null || title.isBlank()) continue;

            Article article = new Article();
            article.setOutlet(outlet);
            article.setGuid(guid);
            article.setTitle(title);
            article.setUrl(entry.getLink() != null ? entry.getLink() : "");
            article.setPublishedAt(toLocalDateTime(entry.getPublishedDate()));
            article.setIngestedAt(LocalDateTime.now());

            // Embed immediately
            float[] vec = embeddingService.embedOne(title);
            article.setEmbedding(EmbeddingService.toBytes(vec));

            articleRepository.save(article);
            newCount++;
        }

        log.info("[{}] {} new articles", outlet.getSlug(), newCount);
        return newCount;
    }

    private String cleanTitle(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("<[^>]+>", "")
                  .replaceAll("&[a-z]+;", " ")
                  .replaceAll("\\s+", " ")
                  .strip();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return LocalDateTime.now();
        return date.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
    }
}
