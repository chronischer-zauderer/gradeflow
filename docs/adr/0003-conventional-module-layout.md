# ADR 0003: Conventional module layout

## Context

The project prioritizes a simple structure that is easy to navigate.

## Decision

Organize capability modules by conventional responsibilities such as
`controller`, `schema`, `service`, `repository`, and `entity`.

## Consequences

The architecture has less ceremony than strict hexagonal layering while still
keeping business capabilities and deployable applications separate.
