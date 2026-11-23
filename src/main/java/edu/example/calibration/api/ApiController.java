package edu.example.calibration.api;

import edu.example.calibration.async.AsyncJobRegistry;
import edu.example.calibration.enrollment.Enrollment;
import edu.example.calibration.enrollment.EnrollmentRepository;
import edu.example.calibration.enrollment.EnrollmentService;
import edu.example.calibration.events.ConfirmationRepository;
import edu.example.calibration.events.ListenerExecutionRepository;
import edu.example.calibration.jms.*;
import edu.example.calibration.scheduling.ConfigurableCleanupJob;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    public record EnrollRequest(@NotBlank String studentId, @NotBlank String courseId,
                                boolean forceRollback) {}
    public record SendMessageRequest(String messageId, Long enrollmentId,
            @NotBlank String studentId, @NotBlank String courseId,
            boolean failFirstAttempt) {}

    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollments;
    private final ConfirmationRepository confirmations;
    private final ListenerExecutionRepository listeners;
    private final EnrollmentMessageSender sender;
    private final DeliveryAttemptRepository attempts;
    private final ReceivedMessageRepository receipts;
    private final AsyncJobRegistry jobs;
    private final ConfigurableCleanupJob cleanup;

    public ApiController(EnrollmentService enrollmentService, EnrollmentRepository enrollments,
            ConfirmationRepository confirmations, ListenerExecutionRepository listeners,
            EnrollmentMessageSender sender, DeliveryAttemptRepository attempts,
            ReceivedMessageRepository receipts, AsyncJobRegistry jobs, ConfigurableCleanupJob cleanup) {
        this.enrollmentService = enrollmentService;
        this.enrollments = enrollments;
        this.confirmations = confirmations;
        this.listeners = listeners;
        this.sender = sender;
        this.attempts = attempts;
        this.receipts = receipts;
        this.jobs = jobs;
        this.cleanup = cleanup;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<?> enroll(@Valid @RequestBody EnrollRequest request) {
        try {
            Enrollment result = enrollmentService.enroll(
                    request.studentId(), request.courseId(), request.forceRollback());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "enrollmentId", result.getId(), "studentId", result.getStudentId(),
                    "courseId", result.getCourseId(), "status", "COMMITTED"));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "ROLLED_BACK", "reason", exception.getMessage()));
        }
    }

    @GetMapping("/enrollments/{id}")
    public ResponseEntity<?> enrollment(@PathVariable Long id) {
        return enrollments.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/enrollments/{id}/side-effects")
    public Object sideEffects(@PathVariable Long id) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("confirmation", confirmations.findByEnrollmentId(id).orElse(null));
        result.put("listenerExecutions", listeners.findByEnrollmentIdOrderByIdAsc(id));
        return result;
    }

    @PostMapping("/messages")
    public ResponseEntity<?> send(@Valid @RequestBody SendMessageRequest request) {
        String id = request.messageId() == null || request.messageId().isBlank()
                ? UUID.randomUUID().toString() : request.messageId();
        sender.send(new EnrollmentMessage(id, request.enrollmentId(), request.studentId(),
                request.courseId(), request.failFirstAttempt()));
        return ResponseEntity.accepted().body(Map.of("messageId", id, "status", "QUEUED"));
    }

    @GetMapping("/messages/{id}")
    public Object message(@PathVariable String id) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("messageId", id);
        result.put("attempts", attempts.findByMessageIdOrderByDeliveryCountAsc(id));
        result.put("received", receipts.findById(id).orElse(null));
        return result;
    }

    @PostMapping("/async/jobs")
    public ResponseEntity<?> startAsync() {
        return ResponseEntity.accepted().body(Map.of("jobId", jobs.start(), "status", "SUBMITTED"));
    }

    @GetMapping("/async/jobs/{id}")
    public ResponseEntity<?> getAsync(@PathVariable String id) {
        Object state = jobs.status(id);
        return state == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(state);
    }

    @GetMapping("/scheduled/status")
    public Object scheduledStatus() {
        return new ScheduleStatus(cleanup.getRuns(), cleanup.getLastRun());
    }
    public record ScheduleStatus(int runCount, java.time.Instant lastRun) {}
}
