package edu.example.calibration.jms;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EnrollmentMessageConsumer {
    private final JsonMapper mapper;
    private final JmsRecordWriter writer;

    public EnrollmentMessageConsumer(JsonMapper mapper, JmsRecordWriter writer) {
        this.mapper = mapper;
        this.writer = writer;
    }

    @JmsListener(destination = "${calibration.jms.destination}",
                 containerFactory = "jmsListenerContainerFactory")
    public void receive(TextMessage message) throws JMSException {
        EnrollmentMessage payload = mapper.readValue(message.getText(), EnrollmentMessage.class);
        int deliveryCount = message.propertyExists("JMSXDeliveryCount")
                ? message.getIntProperty("JMSXDeliveryCount") : 1;

        if (payload.failFirstAttempt() && deliveryCount == 1) {
            writer.recordAttempt(payload.messageId(), deliveryCount, "FAILED");
            throw new IllegalStateException("Intentional first-delivery failure for redelivery demo");
        }
        writer.recordAttempt(payload.messageId(), deliveryCount, "SUCCESS");
        writer.recordReceipt(payload);
    }
}
