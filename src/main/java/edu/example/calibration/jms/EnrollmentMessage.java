package edu.example.calibration.jms;

public record EnrollmentMessage(
        String messageId,
        Long enrollmentId,
        String studentId,
        String courseId,
        boolean failFirstAttempt) {}
