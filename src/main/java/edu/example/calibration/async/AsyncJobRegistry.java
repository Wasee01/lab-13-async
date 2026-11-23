package edu.example.calibration.async;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AsyncJobRegistry {
    private final SlowAsyncService worker;
    private final Map<String, CompletableFuture<AsyncJobResult>> jobs = new ConcurrentHashMap<>();

    public AsyncJobRegistry(SlowAsyncService worker) { this.worker = worker; }

    public String start() {
        String id = UUID.randomUUID().toString();
        jobs.put(id, worker.slowWork(id));
        return id;
    }

    public Object status(String id) {
        CompletableFuture<AsyncJobResult> future = jobs.get(id);
        if (future == null) return null;
        if (!future.isDone()) return Map.of("jobId", id, "status", "RUNNING");
        if (future.isCompletedExceptionally()) return Map.of("jobId", id, "status", "FAILED");
        return future.join();
    }
}
