package edu.example.calibration.jms;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_attempts", uniqueConstraints =
    @UniqueConstraint(columnNames = {"message_id", "delivery_count"}))
public class DeliveryAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "message_id", nullable = false)
    private String messageId;
    @Column(name = "delivery_count", nullable = false)
    private int deliveryCount;
    @Column(nullable = false)
    private String outcome;
    protected DeliveryAttempt() {}
    public DeliveryAttempt(String messageId, int deliveryCount, String outcome) {
        this.messageId = messageId;
        this.deliveryCount = deliveryCount;
        this.outcome = outcome;
    }
    public Long getId() { return id; }
    public String getMessageId() { return messageId; }
    public int getDeliveryCount() { return deliveryCount; }
    public String getOutcome() { return outcome; }
}
