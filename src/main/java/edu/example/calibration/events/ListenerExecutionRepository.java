package edu.example.calibration.events;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListenerExecutionRepository extends JpaRepository<ListenerExecution, Long> {
    List<ListenerExecution> findByEnrollmentIdOrderByIdAsc(Long enrollmentId);
}
