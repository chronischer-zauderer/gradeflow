# ADR 0001: Modular monolith with worker

## Context

The backend needs HTTP access and asynchronous code evaluation without the
operational cost of independent microservices.

## Decision

Use one Gradle multi-module repository with `app-api` and `app-worker` as the
two deployable processes.

## Consequences

Modules share a release and repository, while API and worker can be deployed
and scaled independently.
