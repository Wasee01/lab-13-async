package edu.example.calibration.jms;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JmsRecordWriter {
    private final DeliveryAttemptRepository attempts;
    private final ReceivedMessageRepository messages;

    public JmsRecordWriter(DeliveryAttemptRepository attempts, ReceivedMessageRepository messages) {
        this.attempts = attempts;
        this.messages = messages;
    }

    // Must persist independently of JMS rollback to show failed delivery history.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(String messageId, int deliveryCount, String outcome) {
        attempts.saveAndFlush(new DeliveryAttempt(messageId, deliveryCount, outcome));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReceipt(EnrollmentMessage payload) {
        messages.saveAndFlush(new ReceivedMessage(payload));
    }
}
