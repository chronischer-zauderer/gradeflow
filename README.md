# GradeFlow Backend

## Importante: Julio no quiere camellar

## **Ahora si, continuemos** 👺👺👺

## Este proyecto es un backend para la aplicación GradeFlow, que permite a los usuarios gestionar y organizar sus calificaciones de manera eficiente.

--- 
### Para ejecutar estamos usando un Makefile
### Comandos disponibles:

## Usar GIT BASH en windows para ejecutar

```bash

GRADLEW := ./gradlew

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
