package edu.example.calibration.events;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmationRepository extends JpaRepository<Confirmation, Long> {
    Optional<Confirmation> findByEnrollmentId(Long enrollmentId);
}
