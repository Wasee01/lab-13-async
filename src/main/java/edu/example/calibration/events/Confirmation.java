package edu.example.calibration.events;

import jakarta.persistence.*;

@Entity
@Table(name = "confirmations")
public class Confirmation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long enrollmentId;
    @Column(nullable = false)
    private String studentId;
    @Column(nullable = false)
    private String courseId;
    protected Confirmation() {}
    public Confirmation(Long enrollmentId, String studentId, String courseId) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
    }
    public Long getId() { return id; }
    public Long getEnrollmentId() { return enrollmentId; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
}
