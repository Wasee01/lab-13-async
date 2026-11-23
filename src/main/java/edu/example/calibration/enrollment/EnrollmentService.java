package edu.example.calibration.enrollment;

import edu.example.calibration.events.EnrollmentSucceededEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    private final EnrollmentRepository repository;
    private final ApplicationEventPublisher publisher;

    public EnrollmentService(EnrollmentRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public Enrollment enroll(String studentId, String courseId, boolean forceRollback) {
        Enrollment saved = repository.saveAndFlush(new Enrollment(studentId, courseId));
        publisher.publishEvent(new EnrollmentSucceededEvent(saved.getId(), studentId, courseId));
        if (forceRollback) {
            throw new IllegalStateException("Simulated enrollment rollback after event publication");
        }
        return saved;
    }
}
