package edu.example.calibration.events;

import edu.example.calibration.jms.EnrollmentMessage;
import edu.example.calibration.jms.EnrollmentMessageSender;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EnrollmentEventListeners {
    private final EventRecordWriter writer;
    private final EnrollmentMessageSender sender;

    public EnrollmentEventListeners(EventRecordWriter writer, EnrollmentMessageSender sender) {
        this.writer = writer;
        this.sender = sender;
    }

    @Order(10)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recordConfirmation(EnrollmentSucceededEvent event) {
        writer.recordConfirmation(event);
    }

    @Order(20)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendMessage(EnrollmentSucceededEvent event) {
        sender.send(new EnrollmentMessage(
                "enrollment-" + event.enrollmentId(),
                event.enrollmentId(), event.studentId(), event.courseId(), false));
        writer.recordJmsPublish(event.enrollmentId());
    }
}
