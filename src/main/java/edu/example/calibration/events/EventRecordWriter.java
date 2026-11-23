package edu.example.calibration.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventRecordWriter {
    private final ConfirmationRepository confirmations;
    private final ListenerExecutionRepository executions;

    public EventRecordWriter(ConfirmationRepository confirmations, ListenerExecutionRepository executions) {
        this.confirmations = confirmations;
        this.executions = executions;
    }

    // AFTER_COMMIT needs a new database transaction to reliably commit writes.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConfirmation(EnrollmentSucceededEvent event) {
        confirmations.save(new Confirmation(event.enrollmentId(), event.studentId(), event.courseId()));
        executions.save(new ListenerExecution(event.enrollmentId(), 10, "CONFIRMATION"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordJmsPublish(Long enrollmentId) {
        executions.save(new ListenerExecution(enrollmentId, 20, "JMS_PUBLISH"));
    }
}
