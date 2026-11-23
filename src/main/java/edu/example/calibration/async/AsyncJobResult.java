package edu.example.calibration.async;

public record AsyncJobResult(String jobId, String executedOnThread, String result) {}
