# Backend architecture

GradeFlow uses a modular monolith with two deployable Spring Boot applications:

- `app-api` exposes HTTP endpoints and owns Flyway migrations.
- `app-worker` consumes RabbitMQ messages and runs asynchronous evaluations.

Modules are organized by business capability. Inside a module, code is grouped
by conventional responsibility (`controller`, `schema`, `service`, `repository`,
`entity`, `mapper`, and `config`) only where that responsibility is needed.

The root Gradle build declares the allowed module dependencies. Applications
assemble modules; business rules remain in the capability modules.
