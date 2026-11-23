package edu.example.calibration.jms;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByMessageIdOrderByDeliveryCountAsc(String messageId);
}
