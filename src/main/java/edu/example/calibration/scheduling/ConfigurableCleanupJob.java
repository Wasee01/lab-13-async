package edu.example.calibration.scheduling;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConfigurableCleanupJob {
    private final AtomicInteger runs = new AtomicInteger();
    private final AtomicReference<Instant> lastRun = new AtomicReference<>();

    // No hard-coded cron expression: '-' means disabled (test profile).
    @Scheduled(cron = "${calibration.cleanup.cron}")
    public void cleanup() {
        runs.incrementAndGet();
        lastRun.set(Instant.now());
    }
    public int getRuns() { return runs.get(); }
    public Instant getLastRun() { return lastRun.get(); }
}
