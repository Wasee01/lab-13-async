# Events, Async & JMS Calibration — Java 21 / Spring Boot 4

## 1. Prerequisites

- JDK 21: `java -version`
- Maven 3.9+: `mvn -version` (or install it through IntelliJ's bundled Maven)
- Docker Desktop running, Linux containers enabled: `docker compose version`
- Postman (optional)
- Ports available: **8080** (API), **3307** (MySQL host mapped to 3306), **61616** (Artemis JMS), **8161** (Artemis browser console)

## 2. Start the app (Windows PowerShell or terminal)

From the project root:

```powershell
docker compose up -d
docker compose ps
mvn clean test
mvn spring-boot:run
```

Run `mvn clean test` **after MySQL and Artemis are ready**. MySQL's health check should report healthy; give Artemis time to finish starting. The test profile has a disabled schedule. If services have not initialized, retry after checking `docker compose logs mysql artemis`.

Or to launch a built JAR:

```powershell
mvn clean package
java -jar target/events-async-jms-calibration-1.0.0.jar
```

Open `http://localhost:8080/api/scheduled/status` and Artemis management console `http://localhost:8161/console` (artemis/artemis). A 404 at `/` is normal: API routes start at `/api`.

## 3. Postman walkthrough

Import **`postman/Events-Async-JMS-Calibration.postman_collection.json`** and run requests in this order:

1. **Task 1 — Commit enrollment**: `POST /api/enrollments` with `forceRollback:false` -> `201 COMMITTED`. Its test script automatically saves `enrollmentId`. Then **View enrollment side effects** -> `confirmation` and `listenerExecutions` (first `CONFIRMATION`, order 10; then `JMS_PUBLISH`, order 20). The message is sent to Artemis and consumed asynchronously.
2. **Task 2 — Force rollback**: `POST /api/enrollments` with `forceRollback:true` -> `409 ROLLED_BACK`. For automated proof, `mvn test` also verifies counts of enrollments, confirmations, and listener calls do not change. The failing transaction has no committed enrollment ID.
3. **Task 3 — Start slow async job**: `POST /api/async/jobs` -> `202 SUBMITTED`, saves `jobId`. **Read async status** -> after ~600 ms the result is `COMPLETED`, with `executedOnThread` beginning `calibration-async-`. Returning `SUBMITTED` alone is *not* the async proof; JUnit also checks `<200 ms` caller time and the executing thread.
4. **Task 4 — Send with retry**: `POST /api/messages` with `failFirstAttempt:true` -> `202 QUEUED` saves `messageId`. **Check message and delivery attempts** -> `FAILED` delivery 1, `SUCCESS` delivery >=2, with every payload field preserved in `received`. The failed attempt is persisted in its own DB transaction, while the JMS transacted session rolls back and Artemis retries.
5. **Task 5 — Scheduler status**: `GET /api/scheduled/status` -> `runCount` and `lastRun`; default cron runs every minute, not immediately. For a quick demo set the env variable `CALIBRATION_CLEANUP_CRON="*/10 * * * * *"` **before starting** the application. Never change the annotation's cron to a literal. Under the `test` profile, the schedule is disabled by `application-test.yml` using Spring's `-` disabled value.

To change the default `baseUrl`, edit the collection variable in Postman. Postman tests validate statuses, ordering, and eventual JMS state (rerun the GET request after the broker has handled the message).

## 4. What the code proves

| Task | Actual mechanism | Inspect | Automated assertion |
|---|---|---|---|
| 1: App events | `ApplicationEventPublisher.publishEvent(EnrollmentSucceededEvent)` + two `@Order` listeners | `EnrollmentService`, `EnrollmentEventListeners` | No entity fields; precisely `CONFIRMATION` then `JMS_PUBLISH` |
| 2: AFTER_COMMIT | Both listeners use `@TransactionalEventListener(phase = AFTER_COMMIT)` | `EnrollmentEventListeners` | Forced rollback leaves no enrollment, confirmation, or listener calls |
| 3: Async | `@EnableAsync`, named `ThreadPoolTaskExecutor`, proxied `@Async("calibrationExecutor")` | `AsyncConfig`, `SlowAsyncService` | Caller `<200 ms`, actual executing thread begins `calibration-async-` |
| 4: JMS | `JmsTemplate.send`, JSON TextMessage, `@JmsListener`, transacted JMS session | `EnrollmentMessageSender`, `EnrollmentMessageConsumer`, `JmsConfig` | Exact round-trip values; failed first delivery and eventual successful redelivery |
| 5: Schedule | `@EnableScheduling`, `@Scheduled(cron = "${calibration.cleanup.cron}")`, test profile override | `ConfigurableCleanupJob`, profile YAML | Cron is a configuration placeholder and zero scheduled runs in `test` |

**Why `REQUIRES_NEW`?** AFTER_COMMIT notifications run after the original transaction is committed; the separate `EventRecordWriter` bean opens independent transactions for the confirmation and execution traces. The JMS failure attempt uses another independent DB transaction so the count survives JMS redelivery. No scheduled cron literal exists in the annotation.

**Important transactional limitation:** An AFTER_COMMIT listener cannot make the external broker send atomic with the MySQL commit. For production at-least-once delivery through outages, add a transactional outbox, retry dispatcher, and idempotent consumer. This calibration demonstrates the requested AFTER_COMMIT suppression and JMS redelivery; it is not an outbox implementation.

## 5. Run every check

```powershell
# Infrastructure must be running
docker compose up -d
mvn clean test
# Expected: Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

Test name: `CalibrationIntegrationTest` (`src/test/java/...`). Unlike mocked tests, these are **real integration tests** that require local MySQL and Artemis. Tests deliberately use unique identifiers to avoid collision. There is no embedded DB and no H2 dependency. The test profile replaces only the scheduling cron.

**Troubleshooting**

- `IllegalStateException: Intentional first-delivery failure for redelivery demo` is expected when Task 4 sends a message with `failFirstAttempt:true`. The exception rolls back the JMS session so Artemis redelivers the message. Use **Check message and delivery attempts** (`GET /api/messages/{messageId}`) to confirm delivery 1 is `FAILED`, a later delivery is `SUCCESS`, and `received` contains the payload. If you only see `FAILED`, repeat the GET after a second.
- `Connection refused` on MySQL: inspect `docker compose ps`, ensure port `3307` is available, and allow the container to become healthy.
- `Could not connect to broker`: inspect `docker compose logs artemis`, verify `localhost:61616` is free. If an old Artemis volume has different credentials, use the credentials originally set or reset the **demo-only** volume.
- `Port 8080 already in use`: set `SERVER_PORT=8081` and change Postman's `baseUrl`.
- Postman GET returns a pending job / no receipt: the async worker and JMS consumer are asynchronous; retry GET after a second. The tests poll for up to 20 seconds for JMS.
- Already running MySQL on 3306: this demo intentionally maps MySQL to **3307**.
- To stop containers without losing data: `docker compose down`. To delete **all demo database/broker data**: `docker compose down -v` (destructive).

## 6. Project structure

- `src/main/java/.../enrollment` — JPA entity, repository, transactional publish
- `.../events` — identifier event, two after-commit listeners, independent confirmation/trace persistence
- `.../async` — named executor, slow async worker, HTTP job registry
- `.../jms` — JSON message, JMS sender/consumer, durable attempt/receipt tables
- `.../scheduling` — configurable cron
- `.../api` — REST endpoints for testing
- `src/test/java/.../CalibrationIntegrationTest.java` — five mechanism-aware integration tests
- `postman/` — importable collection with assertions and variables

## 7. Notes on resetting and configuration

All settings are in `application.yml` with environment variable overrides (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ARTEMIS_URL`, `ARTEMIS_USER`, `ARTEMIS_PASSWORD`, `CALIBRATION_CLEANUP_CRON`). Database schema uses Hibernate `ddl-auto: update` to keep the scratch project easy to run; a real deployment should use migration scripts (Flyway or Liquibase). No authentication is configured because these local demo endpoints intentionally permit causing rollbacks and message failures. Do not expose them to the public Internet.
