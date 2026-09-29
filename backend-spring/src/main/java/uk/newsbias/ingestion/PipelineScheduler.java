package uk.newsbias.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.newsbias.clustering.ClusteringService;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineScheduler {

    private final RssFetcherService  fetcher;
    private final ClusteringService  clusterer;

    private final AtomicReference<LocalDateTime> lastRun = new AtomicReference<>();

    /** Run once on startup after the application is fully ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Running pipeline on startup");
        runPipeline();
    }

    /** Then every 30 minutes (configured via app.fetch-interval-minutes in minutes → ms). */
    @Scheduled(fixedDelayString  = "#{${app.fetch-interval-minutes:30} * 60000}",
               initialDelayString = "#{${app.fetch-interval-minutes:30} * 60000}")
    public void scheduledRun() {
        runPipeline();
    }

    private synchronized void runPipeline() {
        try {
            log.info("Pipeline: fetching RSS feeds");
            int newArticles = fetcher.fetchAll();
            log.info("Pipeline: {} new articles — running clustering", newArticles);
            int newStories = clusterer.runClustering();
            lastRun.set(LocalDateTime.now());
            log.info("Pipeline complete — {} new stories", newStories);
        } catch (Exception e) {
            log.error("Pipeline failed", e);
        }
    }

    public LocalDateTime getLastRun() {
        return lastRun.get();
    }
}
