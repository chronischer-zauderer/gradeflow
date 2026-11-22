# Reporte de cambios: Fase 2 Identity y Courses

## Resumen ejecutivo

Se implementó el núcleo multi-tenant sobre la Fase 1 sin modificar `sandbox-execution`, `app-worker`, `grading` ni `ai-evaluation`.

La iteración incorpora:

- Usuarios persistidos con roles `ADMIN`, `TEACHER` y `STUDENT`.
- Registro de estudiantes/profesores y login con BCrypt.
- JWT stateless configurable mediante `JWT_SECRET` y `JWT_EXPIRATION_MS`.
- Cursos y matrículas mediante códigos de invitación.
- Tareas asociadas a cursos con fecha límite opcional.
- Vinculación de submissions con `student_id`.
- Autorización de submissions por estudiante autor, profesor dueño o administrador.
- Contrato portable `AuthenticatedUser`/`CurrentUserProvider` en `shared-kernel`.
- Contrato OpenAPI y documentación actualizados.

El formato de entrega continúa siendo JSON con `sourceCode`; no se añadió subida de archivos ni almacenamiento externo.

## Archivos creados

| Archivo | Propósito |
|---|---|
| `shared-kernel/src/main/java/com/autograder/shared/UserRole.java` | Enum compartido de roles. |
| `shared-kernel/src/main/java/com/autograder/shared/AuthenticatedUser.java` | Contexto inmutable del usuario autenticado. |
| `shared-kernel/src/main/java/com/autograder/shared/CurrentUserProvider.java` | Puerto para consultar identidad sin acoplar dominio a Spring Security. |
| `shared-kernel/src/main/java/com/autograder/shared/BusinessException.java` | Error de dominio portable con status HTTP. |
| `identity/src/main/java/com/autograder/identity/User.java` | Entidad JPA de usuarios. |
| `identity/src/main/java/com/autograder/identity/UserRepository.java` | Persistencia y búsqueda por email. |
| `identity/src/main/java/com/autograder/identity/JwtTokenService.java` | Emisión y validación de tokens JWT. |
| `identity/src/main/java/com/autograder/identity/AuthService.java` | Registro, login, normalización de email y BCrypt. |
| `identity/src/test/java/com/autograder/identity/AuthServiceTest.java` | Tests de rol ADMIN, duplicados y credenciales inválidas. |
| `assignments/src/main/java/com/autograder/assignments/Course.java` | Entidad de cursos. |
| `assignments/src/main/java/com/autograder/assignments/CourseEnrollment.java` | Entidad de matrículas. |
| `assignments/src/main/java/com/autograder/assignments/CourseRepository.java` | Consultas de cursos por docente/invite code. |
| `assignments/src/main/java/com/autograder/assignments/CourseEnrollmentRepository.java` | Consultas y unicidad lógica de matrículas. |
| `assignments/src/main/java/com/autograder/assignments/CourseService.java` | Reglas de roles, ownership, matrícula y acceso. |
| `assignments/src/test/java/com/autograder/assignments/CourseServiceTest.java` | Tests de aislamiento entre docentes y invite codes inválidos. |
| `app-api/src/main/resources/db/migration/V2__create_users_table.sql` | Tabla `users` y restricciones de roles. |
| `app-api/src/main/resources/db/migration/V3__create_courses_and_assignments.sql` | Cursos, matrículas, evolución de assignments y `submissions.student_id`. |
| `app-api/src/main/java/com/autograder/appapi/SecurityConfig.java` | Seguridad stateless, rutas públicas y handlers 401/403. |
| `app-api/src/main/java/com/autograder/appapi/JwtAuthenticationFilter.java` | Extracción y validación del Bearer JWT. |
| `app-api/src/main/java/com/autograder/appapi/SpringSecurityCurrentUserProvider.java` | Adaptador Spring hacia `CurrentUserProvider`. |
| `app-api/src/main/java/com/autograder/appapi/AuthController.java` | Registro y login. |
| `app-api/src/main/java/com/autograder/appapi/CourseController.java` | Cursos, matrícula y tareas. |
| `app-api/src/test/java/com/autograder/appapi/SubmissionApiIntegrationTest.java` | Flujo autenticado de profesor, estudiante, curso, tarea y submission. |

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `identity/build.gradle` | JPA, BCrypt, JJWT API y dependencias de testing. |
| `assignments/build.gradle` | Dependencia de testing para reglas de cursos. |
| `app-api/build.gradle` | Spring Security y runtime JJWT. |
| `app-api/src/main/resources/application.yml` | Configuración JWT configurable. |
| `app-api/src/main/java/com/autograder/appapi/SubmissionController.java` | Requiere usuario autenticado y aplica autorización en lectura/escritura. |
| `app-api/src/main/java/com/autograder/appapi/GlobalExceptionHandler.java` | Mapea errores de dominio a `400`, `401`, `403`, `404`, `409` y `422`. |
| `assignments/src/main/java/com/autograder/assignments/Assignment.java` | Añade curso, descripción, título y fecha límite conservando constructor legacy. |
| `assignments/src/main/java/com/autograder/assignments/AssignmentRepository.java` | Consulta de tareas por curso. |
| `submissions/src/main/java/com/autograder/submissions/Submission.java` | Añade `studentId` nullable para compatibilidad histórica. |
| `submissions/src/main/java/com/autograder/submissions/SubmissionService.java` | Valida rol, matrícula, fecha límite y permisos de lectura. |
| `submissions/src/test/java/com/autograder/submissions/SubmissionServiceTest.java` | Actualiza inyección del servicio de cursos. |
| `.env.example` | Añade `JWT_SECRET` y `JWT_EXPIRATION_MS`. |
| `api-contract/openapi.yaml` | Añade auth, bearer JWT, cursos, matrículas, tareas y respuestas de seguridad. |
| `README.md` | Actualiza alcance, módulos, JWT, flujo multi-tenant, endpoints y troubleshooting. |
| `docs/Fase1.md` | Sincroniza el estado de Fase 1 y documenta Fase 2. |
| `docs/REPORTE_CAMBIOS_FASE2_IDENTITY_COURSES.md` | Este reporte. |

Este bloque no añadió cambios en `sandbox-execution`, `app-worker`, `grading` ni `ai-evaluation`; cualquier diferencia existente en esos módulos corresponde al trabajo previo de Fase 1/SPI.

## Esquema Flyway

### `users`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | UUID | PK |
| `email` | VARCHAR(320) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL, BCrypt |
| `full_name` | VARCHAR(255) | NOT NULL |
| `role` | VARCHAR(20) | `ADMIN`, `TEACHER`, `STUDENT` |
| `created_at` | TIMESTAMPTZ | NOT NULL |

### `courses`

`id`, `name`, `description`, `invite_code`, `teacher_id`, `created_at`. `invite_code` es único y generado con 8 caracteres alfanuméricos sin caracteres ambiguos.

### `course_enrollments`

`id`, `course_id`, `student_id`, `enrolled_at`, con restricción única `(course_id, student_id)`.

### `assignments`

La migración renombra `name` a `title` y añade `description`, `course_id` nullable y `due_date` nullable. `course_id` queda nullable para conservar la asignación demo de Fase 1.

### `submissions`

Añade `student_id` nullable. Las entregas nuevas de estudiantes reciben el identificador autenticado y requieren matrícula en el curso de la tarea.

## Matriz de permisos

| Operación | ADMIN | TEACHER | STUDENT |
|---|---:|---:|---:|
| Registrar usuario con rol propio | No aplica | Sí | Sí |
| Login | Sí | Sí | Sí |
| Crear curso | Sí | Sí | No |
| Listar cursos | Todos | Propios | Matriculados |
| Unirse a curso | No | No | Sí |
| Crear tarea | Sí | Solo cursos propios | No |
| Listar tareas | Todos | Cursos propios | Cursos matriculados |
| Enviar submission | Sí para pruebas | No | Solo si está matriculado |
| Consultar submission | Todas | Si pertenece a su curso | Solo propias |

## Ejemplos curl

Los comandos requieren `jq` para extraer tokens e identificadores.

### 1. Registrar profesor

```bash
TEACHER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"teacher@example.com","password":"Password123!","fullName":"Teacher One","role":"TEACHER"}' \
  | jq -r .accessToken)
```

### 2. Crear curso

```bash
COURSE=$(curl -s -X POST http://localhost:8080/api/v1/courses \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Algorithms","description":"Core algorithms"}')
COURSE_ID=$(echo "$COURSE" | jq -r .id)
INVITE_CODE=$(echo "$COURSE" | jq -r .inviteCode)
```

### 3. Registrar estudiante y matricularlo

```bash
STUDENT_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"student@example.com","password":"Password123!","fullName":"Student One","role":"STUDENT"}' \
  | jq -r .accessToken)

curl -X POST http://localhost:8080/api/v1/courses/join \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"inviteCode\":\"$INVITE_CODE\"}"
```

### 4. Crear tarea

```bash
ASSIGNMENT=$(curl -s -X POST "http://localhost:8080/api/v1/courses/$COURSE_ID/assignments" \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Exercise 1","description":"Python exercise","language":"PYTHON"}')
ASSIGNMENT_ID=$(echo "$ASSIGNMENT" | jq -r .id)
```

### 5. Enviar y consultar una entrega

```bash
SUBMISSION=$(curl -s -X POST "http://localhost:8080/api/v1/assignments/$ASSIGNMENT_ID/submissions" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sourceCode":"print(\"Hola GradeFlow\")"}')
SUBMISSION_ID=$(echo "$SUBMISSION" | jq -r .submissionId)

curl -H "Authorization: Bearer $STUDENT_TOKEN" \
  "http://localhost:8080/api/v1/submissions/$SUBMISSION_ID"
```

Un usuario no autenticado recibe `401`; un usuario autenticado sin acceso al recurso recibe `403`; un correo duplicado o matrícula repetida recibe `409`; una entrega después de `dueDate` recibe `422`.

## Validación

Todas las validaciones se ejecutaron dentro de Docker con JDK 21. Comando completo:

```bash
docker run --rm \
  --network host \
  -e JAVA_TOOL_OPTIONS=-Dapi.version=1.40 \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -v "$PWD:/workspace" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace \
  eclipse-temurin:21-jdk \
  ./gradlew clean test --no-daemon
```

Resultado:

```text
BUILD SUCCESSFUL in 2m 15s
47 actionable tasks: 47 executed
```

Se validaron `app-api:test`, `app-worker:test`, `identity:test`, `assignments:test`, `submissions:test`, `evaluation-orchestrator:test` y `sandbox-execution:test`. No se dejaron contenedores Testcontainers activos.

## Trazabilidad por fases para Home de la Wiki

```markdown
## Trazabilidad por fases

| Fase | Estado | Implementado | Pendiente |
|---|---|---|---|
| Fase 1: ejecución asíncrona y sandbox | Cerrada | RabbitMQ, worker, PostgreSQL/Flyway, Python Docker, estados de submission y hardening del sandbox | Tuning productivo |
| Fase 2: identity y multi-tenancy | Implementada | JWT stateless, BCrypt, roles, cursos, matrículas, tareas, ownership y submissions vinculadas al estudiante | Administración avanzada de usuarios |
| Fase 2 SPI | Preparada | `ExecutionArtifact`, `GradingPort`, `AiEvaluationPort` y fallbacks | Implementaciones reales de grading e IA |
| Fase 3: grading | Planificada | Módulo y puerto disponibles | Rúbricas, casos de prueba, scoring y reportes |
| Fase 4: feedback IA | Planificada | Módulo y puerto disponibles | Modelo, prompts, validación y persistencia |
| Fases posteriores | Planificada | Módulos reservados para identity extendido y plagio | Frontend, multi-lenguaje, almacenamiento externo y despliegue productivo |
```
