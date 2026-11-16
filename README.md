# GradeFlow Backend

GradeFlow es una plataforma multi-tenant de evaluación automática de código. Esta rama implementa la **Fase 2: identidad, cursos, matrículas y vinculación segura de entregas** sobre el núcleo asíncrono y sandbox de la Fase 1, conforme a la arquitectura SaaS objetivo descrita en la [Wiki de GitHub](https://github.com/chronischer-zauderer/gradeflow/wiki).

El sistema autentica usuarios con JWT, aplica los roles `ADMIN`, `TEACHER` y `STUDENT`, permite crear cursos, matricular estudiantes mediante códigos de invitación, crear tareas y enviar código Python en JSON. Las entregas continúan procesándose de forma asíncrona mediante RabbitMQ y Docker.

## Estado actual

Implementado:

- Registro y login con BCrypt y JWT stateless.
- Cursos, matrículas y tareas con aislamiento por usuario y curso.
- API REST protegida para registrar y consultar entregas.
- Vinculación de cada submission con su estudiante autenticado.
- Persistencia en PostgreSQL mediante JPA y Flyway.
- Procesamiento asíncrono con RabbitMQ y `app-worker`.
- Ejecución de Python 3.12 en Docker sin red y con usuario no privilegiado.
- Límites de tiempo, salida, memoria, swap, CPU, procesos y escritura del sandbox.
- Pruebas unitarias, de integración con Testcontainers y del sandbox real cuando la imagen está disponible.

Fuera del alcance actual:

- Casos de prueba, calificación por rúbrica y reportes.
- Evaluación mediante IA y detección de plagio.
- Frontend, subida de archivos, almacenamiento externo y múltiples lenguajes.
- Despliegue productivo y escalabilidad horizontal.

## Requisitos

- Java 21. El proyecto declara un Gradle Toolchain 21; configura `JAVA_HOME` a un JDK 21 si tu entorno tiene varias versiones.
- Docker Engine o Docker Desktop con Docker Compose.
- Git.
- GNU Make es opcional; también se puede usar directamente `gradlew`.

Verifica el entorno con:

```bash
java -version
docker version
```

## Instalación rápida

Clona el repositorio mediante SSH o HTTPS:

```bash
git clone git@github.com:chronischer-zauderer/gradeflow.git
# o
git clone https://github.com/chronischer-zauderer/gradeflow.git
cd gradeflow
cp .env.example .env
```

En PowerShell:

```powershell
Copy-Item .env.example .env
```

Inicia PostgreSQL y RabbitMQ:

```bash
make infra-up
# alternativa: docker compose --env-file .env -f infra/docker-compose.yml up -d
```

Construye la imagen del sandbox:

```bash
docker build -t gradeflow-python-sandbox infra/sandbox-image/python
```

Compila el proyecto:

```bash
./gradlew clean build
# Windows: .\gradlew.bat clean build
# alternativa: make build
```

Ejecuta cada aplicación en una terminal separada:

```bash
make api       # app-api en http://localhost:8080
make worker    # app-worker, consumidor de RabbitMQ
```

## Arquitectura

El backend es un monolito modular con dos procesos desplegables:

```text
Cliente -> app-api -> PostgreSQL
                  -> RabbitMQ -> app-worker -> sandbox Docker
```

Módulos actuales:

| Módulo | Responsabilidad |
|---|---|
| `shared-kernel` | Tipos compartidos, cola y contratos SPI. |
| `identity` | Usuarios, roles, BCrypt y emisión/validación de JWT. |
| `assignments` | Cursos, matrículas, tareas y reglas de acceso. |
| `submissions` | Entregas, estados, resultados y autorización por matrícula/propiedad. |
| `sandbox-execution` | Puerto y adaptador Docker para ejecutar Python. |
| `evaluation-orchestrator` | Coordina sandbox, grading y feedback. |
| `app-api` | API HTTP, Flyway y publicación en RabbitMQ. |
| `app-worker` | Consumo de mensajes y procesamiento asíncrono. |

`grading`, `ai-evaluation` y `plagiarism` están preparados para fases posteriores. `grading` y `ai-evaluation` proporcionan fallbacks no-op mediante puertos compartidos.

## Sandbox y configuración

El worker ejecuta el código Python con `exec` dentro de un contenedor efímero. El comando mantiene `--network none`, usuario `10001`, timeout y truncamiento de salida, y añade:

| Propiedad | Variable | Valor por defecto |
|---|---|---:|
| Imagen | `SANDBOX_IMAGE` | `gradeflow-python-sandbox` |
| Timeout | `SANDBOX_TIMEOUT_SECONDS` | `10` s |
| Salida máxima | `SANDBOX_MAX_OUTPUT_BYTES` | `65536` bytes |
| Memoria | `SANDBOX_MEMORY` | `256m` |
| Memoria + swap | `SANDBOX_MEMORY_SWAP` | `256m` |
| CPU | `SANDBOX_CPUS` | `0.5` |
| Procesos | `SANDBOX_PIDS_LIMIT` | `64` |
| Solo lectura | `SANDBOX_READ_ONLY` | `true` |
| Temporal | `SANDBOX_TMPFS` | `/tmp:rw,noexec,nosuid,size=64m` |

Estos valores son adecuados para desarrollo local y deben revisarse antes de producción.

## Infraestructura local

| Servicio | Puerto | Credenciales por defecto |
|---|---:|---|
| PostgreSQL | `5432` | base/usuario/contraseña: `autograder` |
| RabbitMQ AMQP | `5672` | usuario/contraseña: `autograder` |
| RabbitMQ Management | `15672` | usuario/contraseña: `autograder` |
| API | `8080` | JWT Bearer en `/api/v1/**`, excepto `/api/v1/auth/**` |

Variables de seguridad:

| Variable | Valor local | Uso |
|---|---|---|
| `JWT_SECRET` | `gradeflow-local-development-secret-change-me-32` | Secreto HMAC de al menos 256 bits. |
| `JWT_EXPIRATION_MS` | `86400000` | Vigencia del token en milisegundos. |

Comprueba el estado con:

```bash
docker compose --env-file .env -f infra/docker-compose.yml ps
```

La consola de RabbitMQ está disponible en `http://localhost:15672`.

## API activa

La migración de Flyway conserva una asignación demo histórica con el identificador `11111111-1111-1111-1111-111111111111`. Esa asignación no pertenece a un curso y se mantiene solo para compatibilidad con datos de Fase 1; las submissions nuevas de estudiantes deben usar una tarea creada dentro de un curso.

Registrar un profesor y guardar su token:

```bash
TEACHER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"teacher@example.com","password":"Password123!","fullName":"Teacher One","role":"TEACHER"}' | jq -r .accessToken)
```

Crear un curso:

```bash
curl -X POST http://localhost:8080/api/v1/courses \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Algorithms","description":"Core"}'
```

La respuesta contiene `id` e `inviteCode`. Un estudiante debe registrarse, usar `POST /api/v1/courses/join` con ese código y crear una tarea como profesor antes de enviar una entrega.

Crear una tarea dentro del curso:

```bash
curl -X POST http://localhost:8080/api/v1/courses/<courseId>/assignments \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Exercise 1","description":"Python exercise","language":"PYTHON"}'
```

El response contiene el `assignmentId` que debe utilizarse en el siguiente request.

Registrar una entrega autenticada:

```bash
curl -X POST http://localhost:8080/api/v1/assignments/<assignmentId>/submissions \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sourceCode":"print(\"Hola GradeFlow\")"}'
```

Respuesta: `202 Accepted` con `submissionId` y estado `QUEUED`.

Consultar una entrega:

```bash
curl -H "Authorization: Bearer $STUDENT_TOKEN" http://localhost:8080/api/v1/submissions/<submissionId>
```

Estados: `QUEUED`, `RUNNING`, `COMPILED`, `COMPILE_ERROR` y `FAILED`. El contrato completo está en [api-contract/openapi.yaml](api-contract/openapi.yaml) y la guía de integración en [docs/frontend-integration.md](docs/frontend-integration.md).

## Comandos

| Comando | Acción |
|---|---|
| `make build` | Compila todo el backend. |
| `make clean` | Elimina salidas de Gradle. |
| `make test` | Ejecuta todas las pruebas. |
| `make assemble` | Ensambla los artefactos sin ejecutar pruebas. |
| `make api` | Ejecuta `app-api`. |
| `make worker` | Ejecuta `app-worker`. |
| `make infra-up` | Inicia PostgreSQL y RabbitMQ. |
| `make infra-down` | Detiene los servicios sin borrar volúmenes. |

## Pruebas

```bash
./gradlew clean test
```

Las pruebas con Testcontainers requieren Docker. La prueba del sandbox real se omite si la imagen `gradeflow-python-sandbox` no está disponible. Para una prueba focalizada:

```bash
./gradlew :sandbox-execution:test
./gradlew :evaluation-orchestrator:test
```

## Troubleshooting

- **Java incorrecto:** configura `JAVA_HOME` a un JDK 21 y confirma con `java -version`.
- **No conecta a PostgreSQL o RabbitMQ:** ejecuta `docker compose --env-file .env -f infra/docker-compose.yml ps` y espera los healthchecks.
- **El worker no procesa:** confirma que `make worker` está ejecutándose y revisa RabbitMQ Management.
- **`401 Unauthorized`:** falta `Authorization: Bearer <token>` o el JWT expiró.
- **`403 Forbidden`:** el rol, curso, matrícula o propiedad de la entrega no permite la operación.
- **`409 Conflict`:** el correo ya existe o el estudiante ya está matriculado.
- **Falta la imagen del sandbox:** ejecuta de nuevo `docker build -t gradeflow-python-sandbox infra/sandbox-image/python`.
- **Puerto ocupado:** cambia `POSTGRES_PORT`, `RABBITMQ_PORT`, `RABBITMQ_MANAGEMENT_PORT` o `SERVER_PORT` en `.env`.
- **Datos locales obsoletos:** `docker compose --env-file .env -f infra/docker-compose.yml down -v` elimina también los volúmenes.

## Documentación

- [Arquitectura actual](docs/architecture.md)
- [Especificación de la Fase 1](docs/Fase1.md)
- [Integración frontend](docs/frontend-integration.md)
- [Contrato OpenAPI](api-contract/openapi.yaml)
- [Decisiones arquitectónicas](docs/adr)
- [Wiki de GitHub](https://github.com/chronischer-zauderer/gradeflow/wiki)
- [Reporte de cambios Fase 1 y SPI](docs/REPORTE_CAMBIOS_FASE1_SPI.md)
- [Reporte de cambios Fase 2](docs/REPORTE_CAMBIOS_FASE2_IDENTITY_COURSES.md)
- [Guía de pruebas dentro de Docker](docs/GUIA_EJECUCION_PRUEBAS_DOCKER.md)

## Licencia

Consulta [LICENSE](LICENSE).
