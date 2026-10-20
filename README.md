# GradeFlow Backend

## Importante: Julio no quiere camellar

## **Ahora si, continuemos** 👺👺👺

## Este proyecto es un backend para la aplicación GradeFlow, que permite a los usuarios gestionar y organizar sus calificaciones de manera eficiente.

--- 
### Para ejecutar estamos usando un Makefile
### Comandos disponibles:

### Usar GIT BASH en windows para ejecutar
### O simplemente instalalo en powershell con el siguiente comando:

```powershell
choco install make -y
```

---
```bash

ifeq ($(OS),Windows_NT)
GRADLEW := .\gradlew.bat
else
GRADLEW := ./gradlew
endif

.PHONY: build clean test assemble api worker infra-up infra-down

build:
	$(GRADLEW) build

clean:
	$(GRADLEW) clean

test:
	$(GRADLEW) test

assemble:
	$(GRADLEW) assemble

api:
	$(GRADLEW) :app-api:bootRun

worker:
	$(GRADLEW) :app-worker:bootRun

infra-up:
	docker compose -f infra/docker-compose.yml up -d

infra-down:
	docker compose -f infra/docker-compose.yml down

```
---

## @TRIVI
# Ya saben mi Gente, aqui su inge de confianza haciendole la vida mas FACIL

---

# Fase 1 — Puesta en marcha

Flujo: el estudiante envía código Python, se encola, el worker lo compila en un sandbox Docker aislado y se consulta el resultado (`COMPILED` / `COMPILE_ERROR`).

## Requisitos
- Java 21 (el Gradle Wrapper ya viene incluido; no hace falta instalar Gradle)
- Docker y Docker Compose
- Git
- Make (opcional)

## Instalación
```bash
git clone <URL_DEL_REPOSITORIO>
cd gradeflow
cp .env.example .env            # PowerShell: Copy-Item .env.example .env
```

## Infraestructura (PostgreSQL 16 + RabbitMQ 3)
```bash
make infra-up                   # o: docker compose -f infra/docker-compose.yml up -d
```

## Imagen del sandbox (Python 3.12)
```bash
docker build -t gradeflow-python-sandbox infra/sandbox-image/python
```

## Compilar el backend
```bash
./gradlew clean build           # Windows: .\gradlew.bat clean build  ·  o: make build
```

## Ejecutar
```bash
make api                        # o: ./gradlew :app-api:bootRun      (HTTP en :8080)
make worker                     # o: ./gradlew :app-worker:bootRun   (consumidor RabbitMQ)
```

## Probar el flujo
Flyway crea las tablas y siembra una asignación demo con id `11111111-1111-1111-1111-111111111111`.
```bash
# Registrar una entrega
curl -X POST http://localhost:8080/api/v1/assignments/11111111-1111-1111-1111-111111111111/submissions \
  -H "Content-Type: application/json" -d '{"sourceCode":"print(\"Hola GradeFlow\")"}'
# -> 202 { "submissionId": "...", "status": "QUEUED" }

# Consultar el estado
curl http://localhost:8080/api/v1/submissions/<submissionId>
# -> { "status": "COMPILED", "result": { "exitCode": 0, ... } }
```

## Pruebas
```bash
./gradlew test
```
Las pruebas unitarias no necesitan Docker. Las de integración usan Testcontainers (PostgreSQL + RabbitMQ) y la prueba del sandbox real necesita Docker con la imagen `gradeflow-python-sandbox`; si Docker no está disponible, esa prueba se omite.

## Detener la infraestructura
```bash
make infra-down                 # o: docker compose -f infra/docker-compose.yml down
```

## Solución de problemas
| Síntoma | Causa / solución |
|---|---|
| `JAVA_HOME`/Java no encontrado | Instala Java 21; el Wrapper usa el toolchain configurado |
| El backend no arranca | Docker detenido → `make infra-up` y espera a que los healthchecks pasen |
| `Connection refused` a 5432/5672 | PostgreSQL/RabbitMQ aún iniciando o puerto ocupado; revisa `docker compose ps` |
| El worker no procesa entregas | RabbitMQ no disponible o worker apagado; revisa logs del worker |
| `COMPILE_ERROR`/`FAILED` inesperado | Falta la imagen del sandbox → vuelve a construirla (ver arriba) |
| Entrega rechazada (400) | Código vacío, demasiado grande, o `assignmentId` inexistente (404) |
| El sandbox tarda demasiado | Supera `SANDBOX_TIMEOUT_SECONDS`; la entrega queda en `FAILED` |
