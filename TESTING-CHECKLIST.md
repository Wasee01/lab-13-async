# Submission / grading checklist

- [ ] `java -version` shows Java 21
- [ ] `docker compose up -d` starts healthy MySQL and Artemis
- [ ] `mvn clean test` reports 5 passing tests
- [ ] `mvn spring-boot:run` starts HTTP 8080
- [ ] Postman Task 1 creates enrollment and two ordered listener rows
- [ ] Postman Task 2 returns 409; integration test proves zero confirmation after rollback
- [ ] Postman Task 3 shows thread prefix `calibration-async-`; JUnit proves `<200 ms` caller
- [ ] Postman Task 4 records `FAILED` then `SUCCESS` attempts and exact received payload
- [ ] Task 5 scheduled status increments in local runtime; test cron is `-`
- [ ] Event record contains ONLY `Long`, `String` identifiers
- [ ] Both event listeners annotate `@TransactionalEventListener(AFTER_COMMIT)` (not plain `@EventListener`)
- [ ] JMS sender uses `JmsTemplate`; receiver uses `@JmsListener`
- [ ] Cron annotation uses `${calibration.cleanup.cron}` with no cron literal

Note: Tests require the real Docker containers, not H2 or any in-memory substitutes.
