package edu.example.calibration.jms;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivedMessageRepository extends JpaRepository<ReceivedMessage, String> {}
