# ADR 0002: Gradle Groovy DSL

## Context

The existing project already uses Gradle Groovy DSL.

## Decision

Keep `settings.gradle` and `build.gradle` and use Gradle Groovy for the
multi-module build.

## Consequences

The setup preserves existing project conventions and avoids an unnecessary DSL
migration.
