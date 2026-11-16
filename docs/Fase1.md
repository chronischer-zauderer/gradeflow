# GradeFlow: Fase 1 y Fase 2

## Fase 1: ejecución asíncrona y sandbox

La Fase 1 recibe código Python en JSON, lo persiste en PostgreSQL, publica el identificador en RabbitMQ y lo ejecuta en un contenedor Docker aislado mediante `app-worker`.

Estados de una entrega:

```text
QUEUED -> RUNNING -> COMPILED
                  -> COMPILE_ERROR
                  -> FAILED
```

El sandbox aplica usuario no privilegiado, red deshabilitada, timeout, límite de salida, memoria, swap, CPU, procesos, filesystem read-only y tmpfs configurable. La implementación ejecuta el código Python con `exec`, por lo que puede producir stdout, stderr y errores de runtime.

## Fase 2: identidad y multi-tenancy

La Fase 2 añade:

- Usuarios con roles `ADMIN`, `TEACHER` y `STUDENT`.
- Registro de estudiantes/profesores y login con BCrypt y JWT stateless.
- Cursos creados por profesores o administradores.
- Matrículas de estudiantes mediante códigos de invitación.
- Tareas asociadas a cursos, con fecha límite opcional.
- `student_id` en submissions.
- Autorización por rol, propietario del curso, matrícula y autor de la entrega.
- Contratos compartidos `AuthenticatedUser`, `CurrentUserProvider` y `UserRole`.

Los endpoints protegidos requieren `Authorization: Bearer <JWT>`. Solo `/api/v1/auth/**`, `/actuator/health` y documentación pública permanecen sin autenticación.

## Migraciones

- `V1__init.sql`: tablas base y asignación demo histórica.
- `V2__create_users_table.sql`: usuarios y roles.
- `V3__create_courses_and_assignments.sql`: cursos, matrículas, evolución de assignments y autor de submissions.

La asignación demo de Fase 1 conserva `course_id` nullable para compatibilidad histórica. Las nuevas submissions de estudiantes requieren una tarea asociada a un curso y una matrícula válida.

## Contratos de API

El contrato actualizado está en [api-contract/openapi.yaml](../api-contract/openapi.yaml). La guía operativa general está en [README.md](../README.md) y el reporte detallado en [REPORTE_CAMBIOS_FASE2_IDENTITY_COURSES.md](REPORTE_CAMBIOS_FASE2_IDENTITY_COURSES.md).

## Fuera de alcance

Todavía no están implementados casos de prueba evaluables, rúbricas, reportes, feedback mediante IA, plagio, frontend, subida de archivos, almacenamiento externo ni soporte multi-lenguaje.
