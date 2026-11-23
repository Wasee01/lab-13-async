package edu.example.calibration.events;

import jakarta.persistence.*;

@Entity
@Table(name = "listener_executions")
public class ListenerExecution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long enrollmentId;
    @Column(nullable = false)
    private int listenerOrder;
    @Column(nullable = false)
    private String listenerName;
    protected ListenerExecution() {}
    public ListenerExecution(Long enrollmentId, int listenerOrder, String listenerName) {
        this.enrollmentId = enrollmentId;
        this.listenerOrder = listenerOrder;
        this.listenerName = listenerName;
    }
    public Long getId() { return id; }
    public Long getEnrollmentId() { return enrollmentId; }
    public int getListenerOrder() { return listenerOrder; }
    public String getListenerName() { return listenerName; }
}
