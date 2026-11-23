package edu.example.calibration.events;

// Immutable identifiers only: NO Enrollment/JPA entity references.
public record EnrollmentSucceededEvent(Long enrollmentId, String studentId, String courseId) {}
