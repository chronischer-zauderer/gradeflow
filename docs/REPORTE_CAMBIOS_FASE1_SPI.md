# Reporte de cambios: Fase 1 y SPI

## Resumen ejecutivo

Se completaron dos bloques de trabajo:

1. **Hardening de Fase 1:** el sandbox Docker ahora aplica límites configurables de memoria, swap, CPU, procesos, escritura, red, usuario, timeout y salida. Se actualizó la documentación local, el contrato OpenAPI y el manejo uniforme de errores HTTP 500.
2. **Contratos internos SPI/Ports:** `shared-kernel` expone DTOs inmutables para ejecución, grading y feedback, junto con `GradingPort` y `AiEvaluationPort`. `grading` y `ai-evaluation` registran fallbacks condicionales. `evaluation-orchestrator` coordina sandbox -> grading -> feedback sin hacer obligatorio el despliegue de las implementaciones reales.

La Fase 1 sigue funcionando como antes: la entrega termina en `COMPILED`, `COMPILE_ERROR` o `FAILED` según el resultado del sandbox y la infraestructura. Las evaluaciones opcionales no alteran ese estado si fallan.

## Archivos creados

| Archivo | Motivo |
|---|---|
| `shared-kernel/src/main/java/com/autograder/shared/contracts/ExecutionArtifact.java` | DTO inmutable del resultado estandarizado del sandbox. |
| `shared-kernel/src/main/java/com/autograder/shared/contracts/GradingResult.java` | DTO inmutable de puntaje, aprobación y resultados por criterio. |
| `shared-kernel/src/main/java/com/autograder/shared/contracts/AiFeedbackResult.java` | DTO inmutable de retroalimentación, sugerencias, confianza y modelo. |
| `shared-kernel/src/main/java/com/autograder/shared/contracts/GradingPort.java` | SPI para conectar una implementación real de grading. |
| `shared-kernel/src/main/java/com/autograder/shared/contracts/AiEvaluationPort.java` | SPI para conectar una implementación real de feedback. |
| `grading/src/main/java/com/autograder/grading/DefaultGradingConfiguration.java` | Fallback Spring `@ConditionalOnMissingBean`. |
| `ai-evaluation/src/main/java/com/autograder/aievaluation/DefaultAiEvaluationConfiguration.java` | Fallback Spring `@ConditionalOnMissingBean`. |

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `README.md` | Reescritura técnica: alcance real de Fase 1, instalación, arquitectura, módulos, sandbox, puertos, API, comandos y troubleshooting. |
| `.env.example` | Añadidas variables de Compose y todos los límites configurables del sandbox. `.env` ya estaba excluido por `.gitignore`; `.env.example` permanece versionable. |
| `sandbox-execution/src/main/java/com/autograder/sandbox/SandboxProperties.java` | Nuevas propiedades de memoria, swap, CPU, PIDs, solo lectura y tmpfs. |
| `sandbox-execution/src/main/java/com/autograder/sandbox/DockerSandboxExecutor.java` | Construcción explícita de flags de aislamiento y recursos mediante `buildCommand`. |
| `sandbox-execution/src/test/java/com/autograder/sandbox/DockerSandboxExecutorTest.java` | Test determinista de flags críticos de `docker run`. |
| `app-worker/src/main/resources/application.yml` | Variables `SANDBOX_MEMORY`, `SANDBOX_MEMORY_SWAP`, `SANDBOX_CPUS`, `SANDBOX_PIDS_LIMIT`, `SANDBOX_READ_ONLY` y `SANDBOX_TMPFS`. |
| `evaluation-orchestrator/src/main/java/com/autograder/orchestrator/EvaluationService.java` | Construcción de `ExecutionArtifact`, invocación ordenada de grading y feedback, y tolerancia a fallos de evaluadores opcionales. |
| `evaluation-orchestrator/src/test/java/com/autograder/orchestrator/EvaluationServiceTest.java` | Mocks de los puertos y verificación del orden sandbox -> grading -> feedback. |
| `grading/build.gradle` | Dependencia de auto-configuración Spring para el fallback. |
| `ai-evaluation/build.gradle` | Dependencia de auto-configuración Spring para el fallback. |
| `app-api/src/main/java/com/autograder/appapi/GlobalExceptionHandler.java` | Respuesta uniforme `500 {"error":"Internal server error"}`. |
| `api-contract/openapi.yaml` | `servers`, ejemplos, errores 400/404/500, esquema de error, estados y límites del request. |
| `docs/Fase1.md` | Corrección del estado: el flujo ya está implementado y endurecido; se documentan sus estados, límites y SPI. |
| `docs/REPORTE_CAMBIOS_FASE1_SPI.md` | Este reporte. |

No se eliminaron archivos del repositorio.

## Seguridad del sandbox

`DockerSandboxExecutor` ejecuta Python mediante `exec` dentro de un contenedor efímero. Los flags efectivos son:

| Flag | Propiedad | Valor por defecto |
|---|---|---|
| `--network none` | fijo | Red deshabilitada |
| `--user 10001` | fijo | Usuario no privilegiado |
| `--memory 256m` | `SANDBOX_MEMORY` | `256m` |
| `--memory-swap 256m` | `SANDBOX_MEMORY_SWAP` | `256m` |
| `--cpus 0.5` | `SANDBOX_CPUS` | `0.5` |
| `--pids-limit 64` | `SANDBOX_PIDS_LIMIT` | `64` |
| `--read-only` | `SANDBOX_READ_ONLY=true` | Activado |
| `--tmpfs /tmp:rw,noexec,nosuid,size=64m` | `SANDBOX_TMPFS` | Activado |
| timeout del proceso | `SANDBOX_TIMEOUT_SECONDS` | `10` segundos |
| truncamiento de stdout/stderr | `SANDBOX_MAX_OUTPUT_BYTES` | `65536` bytes |

`--memory-swap` igual a `--memory` evita swap adicional cuando Docker aplica ambas restricciones. `--read-only` bloquea escritura persistente; `/tmp` queda disponible únicamente con el tmpfs configurado.

## Guía de integración SPI

### `ExecutionArtifact`

Firma exacta:

```java
public record ExecutionArtifact(
        UUID submissionId,
        UUID assignmentId,
        String sourceCode,
        String stdout,
        String stderr,
        int exitCode,
        long executionTimeMs,
        boolean timedOut,
        boolean compiled) {
}
```

### `GradingResult`

Firma exacta:

```java
public record GradingResult(
        double score,
        double maximumScore,
        boolean approved,
        List<CriterionResult> criteria,
        String summary) {

    public record CriterionResult(
            String criterion,
            double score,
            double maximumScore,
            boolean passed,
            String summary) {
    }
}
```

El constructor normaliza `criteria == null` a una lista vacía, copia defensivamente la lista y normaliza `summary == null` a una cadena vacía.

### `AiFeedbackResult`

Firma exacta:

```java
public record AiFeedbackResult(
        String feedback,
        List<String> suggestions,
        double confidence,
        String model) {
}
```

El constructor copia defensivamente `suggestions` y normaliza valores de texto nulos.

### Puertos

```java
public interface GradingPort {
    GradingResult evaluate(ExecutionArtifact artifact);
}

public interface AiEvaluationPort {
    AiFeedbackResult generateFeedback(
            ExecutionArtifact artifact,
            GradingResult gradingResult);
}
```

### Registrar una implementación real

El compañero puede crear una implementación Spring normal en su módulo:

```java
@Service
public class RubricGradingService implements GradingPort {
    @Override
    public GradingResult evaluate(ExecutionArtifact artifact) {
        // Implementación real del grading.
    }
}
```

Para feedback:

```java
@Service
public class ModelAiEvaluationService implements AiEvaluationPort {
    @Override
    public AiFeedbackResult generateFeedback(
            ExecutionArtifact artifact,
            GradingResult gradingResult) {
        // Implementación real de feedback.
    }
}
```

El componente debe estar bajo `com.autograder` para que las aplicaciones actuales lo detecten mediante component scan. Al existir un bean real del tipo correspondiente, `@ConditionalOnMissingBean` evita registrar el fallback. No es necesario modificar `DockerSandboxExecutor`, `app-worker` ni las firmas del listener.

El orden del orquestador es:

```text
SandboxExecutor.compile(sourceCode)
    -> ExecutionArtifact
GradingPort.evaluate(artifact)
    -> GradingResult
AiEvaluationPort.generateFeedback(artifact, gradingResult)
    -> persistencia del estado de ejecución
```

Si grading o IA lanzan una excepción de runtime, el error se registra como warning y el estado del sandbox se conserva. Los fallos del sandbox siguen produciendo `FAILED`.

## Validación ejecutada

Todas las validaciones se ejecutaron dentro de contenedores efímeros. No se ejecutó Gradle ni una aplicación directamente en el host.

### Prueba focalizada del sandbox

```bash
docker run --rm \
  -v "$PWD:/workspace" \
  -w /workspace \
  eclipse-temurin:21-jdk \
  ./gradlew :sandbox-execution:test \
    --tests com.autograder.sandbox.DockerSandboxExecutorTest
```

Resultado: `BUILD SUCCESSFUL`.

### Prueba focalizada del orquestador

```bash
docker run --rm \
  -v "$PWD:/workspace" \
  -w /workspace \
  eclipse-temurin:21-jdk \
  ./gradlew :evaluation-orchestrator:test \
    --tests com.autograder.orchestrator.EvaluationServiceTest
```

Resultado: `BUILD SUCCESSFUL`.

### Suite completa

El intento inicial de `./gradlew clean test` dentro de Docker compiló, pero Testcontainers no pudo negociar con el daemon porque el cliente Docker Java usó API `1.32` y el daemon exige como mínimo `1.40`.

La suite completa pasó usando un contenedor JDK 21 con configuración explícita de API Docker, Ryuk deshabilitado para el entorno efímero y red host para acceder a los puertos publicados por Testcontainers:

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

Resultado final:

```text
BUILD SUCCESSFUL
42 actionable tasks: 40 executed, 2 up-to-date
```

Se ejecutaron `app-api:test`, `app-worker:test`, `evaluation-orchestrator:test`, `sandbox-execution:test` y `submissions:test`. Durante el cierre de Testcontainers aparecen warnings de conexiones cerradas de PostgreSQL/RabbitMQ, pero no provocan fallos y el proceso termina correctamente.

También se ejecutó la validación sin las suites que requieren Testcontainers:

```bash
docker run --rm \
  -v "$PWD:/workspace" \
  -w /workspace \
  eclipse-temurin:21-jdk \
  ./gradlew clean test -x :app-api:test -x :app-worker:test
```

Resultado: `BUILD SUCCESSFUL`.

## Trazabilidad por fases para Home de la Wiki

El bloque siguiente está listo para copiar en el `Home` de la Wiki:

```markdown
## Trazabilidad por fases

| Fase | Estado | Implementado en este repositorio | Fuera de alcance |
|---|---|---|---|
| Fase 1: Core de ejecución asíncrona y sandbox | Implementada | API de submissions, PostgreSQL/Flyway, RabbitMQ, worker, Python 3.12 en Docker, estados QUEUED/RUNNING/COMPILED/COMPILE_ERROR/FAILED | Auth, calificación, IA, plagio, frontend y producción |
| Fase 1 Hardening | Implementada | Red deshabilitada, usuario no privilegiado, timeout, salida limitada, memoria, swap, CPU, PIDs, filesystem read-only y tmpfs | Threat modeling y tuning productivo |
| Fase 1 SPI | Preparada | `ExecutionArtifact`, `GradingResult`, `AiFeedbackResult`, `GradingPort`, `AiEvaluationPort` y fallbacks condicionales | Implementaciones reales de rúbrica y modelo IA |
| Fase 2: Grading | Preparada para integración | Módulo `grading` y puerto compartido listos para recibir una implementación `@Service` | Reglas de rúbrica y persistencia de reportes |
| Fase 3: AI feedback | Preparada para integración | Módulo `ai-evaluation` y puerto compartido listos para recibir una implementación `@Service` | Proveedor de modelo, prompts, validación y almacenamiento |
| Fases posteriores | Planificada | Módulos reservados: `identity`, `plagiarism` y extensiones del dominio | Autenticación, frontend, plagio, multi-lenguaje y despliegue productivo |
```
