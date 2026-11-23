package edu.example.calibration.jms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class EnrollmentMessageSender {
    private final JmsTemplate template;
    private final JsonMapper mapper;
    private final String destination;

    public EnrollmentMessageSender(JmsTemplate template, JsonMapper mapper,
                                   @Value("${calibration.jms.destination}") String destination) {
        this.template = template;
        this.mapper = mapper;
        this.destination = destination;
    }

    public void send(EnrollmentMessage payload) {
        // A JSON TextMessage with all typed payload fields; no Java serialization.
        String json = mapper.writeValueAsString(payload);
        template.send(destination, session -> session.createTextMessage(json));
    }
}
