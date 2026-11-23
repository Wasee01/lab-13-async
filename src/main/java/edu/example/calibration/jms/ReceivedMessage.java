package edu.example.calibration.jms;

import jakarta.persistence.*;

@Entity
@Table(name = "received_messages")
public class ReceivedMessage {
    @Id @Column(nullable = false)
    private String messageId;
    @Column
    private Long enrollmentId;
    @Column(nullable = false)
    private String studentId;
    @Column(nullable = false)
    private String courseId;
    @Column(nullable = false)
    private boolean failFirstAttempt;
    protected ReceivedMessage() {}
    public ReceivedMessage(EnrollmentMessage message) {
        this.messageId = message.messageId();
        this.enrollmentId = message.enrollmentId();
        this.studentId = message.studentId();
        this.courseId = message.courseId();
        this.failFirstAttempt = message.failFirstAttempt();
    }
    public String getMessageId() { return messageId; }
    public Long getEnrollmentId() { return enrollmentId; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public boolean isFailFirstAttempt() { return failFirstAttempt; }
}
