package edu.example.calibration;

import edu.example.calibration.async.SlowAsyncService;
import edu.example.calibration.enrollment.EnrollmentRepository;
import edu.example.calibration.enrollment.EnrollmentService;
import edu.example.calibration.events.*;
import edu.example.calibration.jms.*;
import edu.example.calibration.scheduling.ConfigurableCleanupJob;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** Integration tests use real MySQL + Artemis supplied by compose.yaml. */
@SpringBootTest
@ActiveProfiles("test")
class CalibrationIntegrationTest {
    @Autowired EnrollmentService enrollmentsService;
    @Autowired EnrollmentRepository enrollments;
    @Autowired ConfirmationRepository confirmations;
    @Autowired ListenerExecutionRepository listenerExecutions;
    @Autowired EnrollmentMessageSender sender;
    @Autowired DeliveryAttemptRepository attempts;
    @Autowired ReceivedMessageRepository received;
    @Autowired SlowAsyncService async;
    @Autowired ConfigurableCleanupJob cleanup;
    @Autowired Environment environment;

    @Test
    void task1_eventHasOnlyIdentifiers_andBothListenersExecuteInOrder() {
        assertTrue(EnrollmentSucceededEvent.class.isRecord());
        for (RecordComponent component : EnrollmentSucceededEvent.class.getRecordComponents()) {
            assertTrue(component.getType().isPrimitive()
                            || component.getType() == Long.class
                            || component.getType() == String.class,
                    "Event must contain identifiers, not entities: " + component.getName());
        }
        Long id = enrollmentsService.enroll("student-" + UUID.randomUUID(), "JAVA-21", false).getId();
        assertEquals(1, confirmations.findByEnrollmentId(id).stream().count());
        List<ListenerExecution> calls = listenerExecutions.findByEnrollmentIdOrderByIdAsc(id);
        assertEquals(List.of("CONFIRMATION", "JMS_PUBLISH"),
                calls.stream().map(ListenerExecution::getListenerName).toList());
        assertEquals(List.of(10, 20), calls.stream().map(ListenerExecution::getListenerOrder).toList());
    }

    @Test
    void task2_rolledBackEnrollmentDoesNotSendConfirmationOrJmsMessage() throws Exception {
        long beforeEnrollments = enrollments.count();
        long beforeConfirmations = confirmations.count();
        long beforeExecutions = listenerExecutions.count();
        String student = "rollback-" + UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                () -> enrollmentsService.enroll(student, "SQL-101", true));
        Thread.sleep(250); // any incorrectly eager listener would already have run
        assertEquals(beforeEnrollments, enrollments.count());
        assertEquals(beforeConfirmations, confirmations.count());
        assertEquals(beforeExecutions, listenerExecutions.count());
    }

    @Test
    void task3_asyncRunsOnNamedPoolAndInvocationReturnsUnder200ms() throws Exception {
        long started = System.nanoTime();
        var future = async.slowWork(UUID.randomUUID().toString());
        long callerMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(callerMillis < 200, "@Async must release caller in <200ms, was " + callerMillis);
        assertFalse(future.isDone(), "600ms worker should still be executing");
        var result = future.get(4, TimeUnit.SECONDS);
        assertEquals("COMPLETED", result.result());
        assertTrue(result.executedOnThread().startsWith("calibration-async-"),
                "Unexpected worker thread: " + result.executedOnThread());
        assertNotEquals(Thread.currentThread().getName(), result.executedOnThread());
    }

    @Test
    void task4_jmsRoundTripPreservesAllFieldsAndRecordsRedelivery() throws Exception {
        String id = "retry-" + UUID.randomUUID();
        EnrollmentMessage original = new EnrollmentMessage(id, 472L,
                "student-jms", "MESSAGING-101", true);
        sender.send(original);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (received.findById(id).isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }
        ReceivedMessage actual = received.findById(id).orElseThrow();
        assertEquals(original.messageId(), actual.getMessageId());
        assertEquals(original.enrollmentId(), actual.getEnrollmentId());
        assertEquals(original.studentId(), actual.getStudentId());
        assertEquals(original.courseId(), actual.getCourseId());
        assertEquals(original.failFirstAttempt(), actual.isFailFirstAttempt());
        var history = attempts.findByMessageIdOrderByDeliveryCountAsc(id);
        assertTrue(history.size() >= 2, "Broker must redeliver failed message");
        assertEquals("FAILED", history.getFirst().getOutcome());
        assertEquals(1, history.getFirst().getDeliveryCount());
        assertEquals("SUCCESS", history.getLast().getOutcome());
        assertTrue(history.getLast().getDeliveryCount() >= 2);
    }

    @Test
    void task5_schedulerCronIsConfigurationDrivenAndDisabledInTest() throws Exception {
        assertEquals("-", environment.getRequiredProperty("calibration.cleanup.cron"));
        int before = cleanup.getRuns();
        Thread.sleep(1200);
        assertEquals(before, cleanup.getRuns(), "Test profile must not schedule the job");
        var annotation = ConfigurableCleanupJob.class.getMethod("cleanup")
                .getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        assertEquals("${calibration.cleanup.cron}", annotation.cron());
    }
}
