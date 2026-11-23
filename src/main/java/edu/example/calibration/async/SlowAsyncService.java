package edu.example.calibration.async;

import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SlowAsyncService {
    @Async("calibrationExecutor")
    public CompletableFuture<AsyncJobResult> slowWork(String jobId) {
        try {
            Thread.sleep(600);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(exception);
        }
        return CompletableFuture.completedFuture(
                new AsyncJobResult(jobId, Thread.currentThread().getName(), "COMPLETED"));
    }
}
