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