package edu.example.calibration.enrollment;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollments")
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String studentId;
    @Column(nullable = false)
    private String courseId;

    protected Enrollment() {}

    public Enrollment(String studentId, String courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }
    public Long getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
}
