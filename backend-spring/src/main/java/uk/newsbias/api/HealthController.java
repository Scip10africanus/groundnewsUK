package uk.newsbias.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.newsbias.ingestion.PipelineScheduler;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final PipelineScheduler scheduler;

    @GetMapping("/health")
    public Map<String, Object> health() {
        LocalDateTime lastRun = scheduler.getLastRun();
        return Map.of(
                "status",     "ok",
                "last_fetch", lastRun != null ? lastRun.toString() : "not yet run"
        );
    }
}
