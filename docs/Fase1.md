# GradeFlow — Fase 1: Compilación aislada de entregas

## 1. Objetivo

El objetivo de la Fase 1 es construir una primera versión funcional del flujo de evaluación automática de GradeFlow.

Al finalizar esta fase, el sistema debe permitir:

1. Recibir una entrega de código de un estudiante.
2. Asociar la entrega con una asignación.
3. Registrar la entrega y su estado.
4. Enviar la entrega al proceso de evaluación asíncrona.
5. Ejecutar la compilación o validación del código dentro de un sandbox aislado.
6. Capturar la salida estándar, la salida de error y el código de salida.
7. Determinar si la compilación fue exitosa o fallida.
8. Consultar el estado y el resultado de una entrega.
9. Persistir la información necesaria en PostgreSQL.
10. Ejecutar el flujo completo mediante pruebas automáticas.

El requisito indispensable de esta fase es:

> Dado un código enviado por un estudiante, GradeFlow debe poder procesarlo en un entorno aislado y determinar si compila correctamente.

La Fase 1 no pretende implementar todavía calificaciones, análisis de calidad, detección de plagio ni evaluación mediante inteligencia artificial.

---

## 2. Estado actual del proyecto

El repositorio ya cuenta con una base técnica inicial:

- Java 21.
- Gradle Groovy multi-módulo.
- Spring Boot 3.5.6.
- Dos aplicaciones ejecutables:
    - `app-api`: expone la API HTTP.
    - `app-worker`: procesa evaluaciones asíncronas.
- PostgreSQL 16 mediante Docker Compose.
- RabbitMQ 3 mediante Docker Compose.
- Imagen Docker para ejecución aislada de Python 3.12.
- Migraciones gestionadas mediante Flyway desde `app-api`.
- Contrato inicial en `api-contract/openapi.yaml`.
- Workflow de GitHub Actions en `.github/workflows/ci.yml`.
- Comandos auxiliares definidos en el `Makefile`.

Los módulos actuales son:

```text
shared-kernel
identity
assignments
submissions
sandbox-execution
ai-evaluation
grading
plagiarism
evaluation-orchestrator
app-api
app-worker
```

Actualmente existen los puntos de entrada de `app-api` y `app-worker`, pero todavía no está implementado el flujo funcional completo de una entrega.

---

## 3. Alcance de la fase

### 3.1. Incluido

La Fase 1 incluye:

- Configuración reproducible del proyecto.
- Compilación del backend completo.
- Modelo mínimo de asignaciones y entregas.
- Endpoint para registrar una entrega.
- Persistencia de entregas y resultados.
- Publicación de trabajos de evaluación en RabbitMQ.
- Consumo de trabajos desde `app-worker`.
- Ejecución del código en un sandbox Docker.
- Validación de compilación para Python 3.12.
- Captura de:
    - Estado de la compilación.
    - Código de salida.
    - Salida estándar.
    - Salida de error.
    - Duración de la ejecución.
- Consulta del estado de una entrega.
- Pruebas unitarias.
- Pruebas de integración con PostgreSQL y RabbitMQ.
- Integración continua.
- Documentación para levantar el entorno local.

### 3.2. No incluido

No se incluye todavía:

- Autenticación completa.
- Registro y administración de usuarios.
- Roles y permisos definitivos.
- Calificación automática.
- Ejecución de pruebas funcionales del estudiante.
- Compilación de múltiples lenguajes.
- Soporte para dependencias externas definidas por el estudiante.
- Detección de plagio.
- Evaluación mediante inteligencia artificial.
- Interfaz web.
- Informes avanzados.
- Despliegue productivo.
- Escalabilidad horizontal.
- Optimización avanzada del sandbox.

El primer lenguaje soportado por el sandbox será **Python 3.12**, debido a que el repositorio ya contiene la imagen:

```text
infra/sandbox-image/python/Dockerfile
```

En Python, la fase de compilación se validará inicialmente mediante:

```bash
python -m py_compile <archivo.py>
```

La arquitectura debe permitir añadir posteriormente Java, JavaScript u otros lenguajes sin modificar el flujo principal de entregas.

---

## 4. Flujo funcional esperado

El flujo mínimo será el siguiente:

```text
Cliente
  |
  | POST /api/v1/assignments/{assignmentId}/submissions
  |
app-api
  |
  | Guarda la entrega con estado QUEUED
  | Publica un mensaje en RabbitMQ
  |
app-worker
  |
  | Consume el mensaje
  | Ejecuta el código en el sandbox
  | Guarda el resultado
  |
PostgreSQL
  |
  | Estado y resultado de la entrega
  |
Cliente
  |
  | GET /api/v1/submissions/{submissionId}
  |
Resultado de compilación
```

Estados mínimos de una entrega:

```text
QUEUED
RUNNING
COMPILED
COMPILE_ERROR
FAILED
```

El estado `COMPILED` indica que el código pudo compilarse correctamente. El estado `COMPILE_ERROR` indica que el código fue procesado, pero presentó errores de compilación o sintaxis.

---

## 5. Decisiones técnicas

### 5.1. Backend

```text
Lenguaje: Java
Versión: 21
Framework: Spring Boot 3.5.6
Sistema de compilación: Gradle 9.7.1 mediante Gradle Wrapper
```

Motivos:

- Ya es la tecnología utilizada por el repositorio.
- Tiene soporte maduro para APIs HTTP, persistencia, mensajería y testing.
- Se integra correctamente con GitHub Actions.
- Permite separar la API y el procesamiento asíncrono.
- El Gradle Wrapper permite compilar sin depender de una instalación local específica de Gradle.

### 5.2. Arquitectura

Se mantiene un monolito modular con dos procesos ejecutables:

```text
app-api
  Responsabilidades:
  - Recibir solicitudes HTTP.
  - Validar entregas.
  - Persistir entregas.
  - Publicar trabajos en RabbitMQ.
  - Consultar estados y resultados.

app-worker
  Responsabilidades:
  - Consumir trabajos.
  - Ejecutar compilaciones.
  - Comunicarse con el sandbox.
  - Persistir resultados.
```

No se crearán microservicios independientes durante esta fase.

### 5.3. Base de datos

```text
Base de datos: PostgreSQL
Versión: 16
Migraciones: Flyway
```

PostgreSQL se utilizará para almacenar las entidades necesarias del flujo de entregas. No se utilizará una tabla genérica `example_records`, ya que no representa el dominio principal de GradeFlow.

### 5.4. Mensajería

```text
Broker: RabbitMQ
Versión: 3
```

RabbitMQ se utilizará para desacoplar la recepción de la entrega de su procesamiento.

### 5.5. Sandbox

El código del estudiante nunca debe ejecutarse directamente dentro de `app-api` o `app-worker`.

El sandbox debe:

- Ejecutarse dentro de un contenedor.
- Utilizar un usuario sin privilegios.
- Montar únicamente el directorio de trabajo necesario.
- No incluir credenciales de la aplicación.
- Tener límites de tiempo.
- Tener límites de memoria y CPU cuando el entorno lo permita.
- No utilizar una red externa durante la compilación.
- Devolver un resultado controlado al worker.

---

## 6. Estructura relevante

La estructura esperada será:

```text
gradeflow/
├── README.md
├── Makefile
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── .env.example
├── infra/
│   ├── docker-compose.yml
│   └── sandbox-image/
│       └── python/
│           └── Dockerfile
├── database/
│   └── migrations/
├── api-contract/
│   └── openapi.yaml
├── docs/
│   ├── Fase1.md
│   ├── architecture.md
│   └── adr/
├── app-api/
├── app-worker/
├── submissions/
├── assignments/
├── sandbox-execution/
├── evaluation-orchestrator/
└── .github/
    └── workflows/
        └── ci.yml
```

La organización interna de cada módulo debe seguir las responsabilidades que realmente necesite:

```text
controller/
schema/
service/
repository/
entity/
mapper/
config/
```

No es necesario crear directorios vacíos ni clases sin comportamiento únicamente para cumplir una estructura teórica.

---

## 7. Modelo mínimo

### 7.1. Assignment

Representa una asignación o ejercicio al que pertenece una entrega.

Campos mínimos:

```text
id
name
language
createdAt
```

Para esta fase, `language` tendrá inicialmente el valor:

```text
PYTHON
```

### 7.2. Submission

Representa el código enviado por un estudiante.

Campos mínimos:

```text
id
assignmentId
sourceCode
status
createdAt
updatedAt
```

El código puede almacenarse inicialmente como texto para simplificar la primera implementación. La decisión deberá revisarse antes de producción, especialmente para entregas grandes.

### 7.3. CompilationResult

Representa el resultado del procesamiento.

Campos mínimos:

```text
id
submissionId
status
exitCode
stdout
stderr
durationMilliseconds
createdAt
```

Las salidas deben tener un tamaño máximo para evitar que una entrega genere registros excesivamente grandes.

---

## 8. API mínima

El contrato inicial debe reflejar el flujo implementado.

### Registrar una entrega

```http
POST /api/v1/assignments/{assignmentId}/submissions
Content-Type: application/json
```

Ejemplo de solicitud:

```json
{
  "sourceCode": "print('Hola GradeFlow')"
}
```

Respuesta esperada:

```http
202 Accepted
```

```json
{
  "submissionId": "uuid",
  "status": "QUEUED"
}
```

### Consultar el estado de una entrega

```http
GET /api/v1/submissions/{submissionId}
```

Respuesta mientras se procesa:

```json
{
  "submissionId": "uuid",
  "status": "RUNNING"
}
```

Respuesta después del procesamiento:

```json
{
  "submissionId": "uuid",
  "status": "COMPILED",
  "result": {
    "exitCode": 0,
    "stdout": "",
    "stderr": "",
    "durationMilliseconds": 120
  }
}
```

Respuesta ante un error de compilación:

```json
{
  "submissionId": "uuid",
  "status": "COMPILE_ERROR",
  "result": {
    "exitCode": 1,
    "stdout": "",
    "stderr": "SyntaxError: invalid syntax",
    "durationMilliseconds": 95
  }
}
```

La API no debe devolver información interna del sistema, credenciales ni rutas sensibles del servidor.

---

## 9. Configuración

Debe existir un archivo `.env.example` con la configuración necesaria para el entorno local:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/autograder
SPRING_DATASOURCE_USERNAME=autograder
SPRING_DATASOURCE_PASSWORD=autograder

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=autograder
RABBITMQ_PASSWORD=autograder

SERVER_PORT=8080

SANDBOX_IMAGE=gradeflow-python-sandbox
SANDBOX_TIMEOUT_SECONDS=10
SANDBOX_MAX_OUTPUT_BYTES=65536
```

Reglas:

- `.env` no debe subirse al repositorio.
- Las contraseñas no deben escribirse en el código.
- Las variables deben poder cambiarse sin recompilar.
- Los valores por defecto solo deben utilizarse para desarrollo local.
- Los logs nunca deben mostrar contraseñas.
- En entornos no locales, las credenciales deben ser obligatorias.

El `.gitignore` debe excluir como mínimo:

```gitignore
.env
.env.local
.env.*.local
```

---

## 10. Infraestructura local

El entorno local debe poder iniciarse con:

```bash
make infra-up
```

o directamente:

```bash
docker compose -f infra/docker-compose.yml up -d
```

El archivo de Docker Compose debe incluir:

- PostgreSQL 16.
- RabbitMQ 3.
- Volúmenes persistentes.
- Variables de entorno configurables.
- Healthchecks.
- Puertos documentados.
- Nombres de servicio estables.

Ejemplo de healthcheck para PostgreSQL:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U autograder -d autograder"]
  interval: 5s
  timeout: 5s
  retries: 10
```

La infraestructura debe detenerse con:

```bash
make infra-down
```

o:

```bash
docker compose -f infra/docker-compose.yml down
```

---

## 11. Migraciones

Las migraciones deben ser administradas por Flyway y ejecutarse desde `app-api`.

La primera migración debe crear las tablas mínimas del flujo:

```text
assignments
submissions
compilation_results
```

La migración debe incluir:

- Claves primarias.
- Relaciones entre tablas.
- Estados controlados.
- Fechas de creación y actualización.
- Índices para consultar entregas por asignación y estado.

No se deben crear tablas genéricas de prueba que no estén relacionadas con el flujo de compilación.

---

## 12. Ejecución del sandbox

El worker debe delegar la compilación a un componente del módulo `sandbox-execution`.

Responsabilidades mínimas:

1. Crear un directorio temporal de trabajo.
2. Escribir el código recibido.
3. Ejecutar el contenedor del lenguaje correspondiente.
4. Aplicar el límite de tiempo.
5. Capturar `stdout`, `stderr` y código de salida.
6. Convertir el resultado a un modelo de dominio.
7. Eliminar los archivos temporales.
8. Informar errores sin ocultarlos ni convertirlos en resultados exitosos.

Para Python 3.12, el comando inicial será equivalente a:

```bash
python -m py_compile Main.py
```

La ejecución del contenedor debe evitar:

- `--privileged`.
- Montajes innecesarios.
- Acceso al socket de Docker.
- Acceso a la red externa.
- Ejecución como `root`.
- Reutilización de directorios entre estudiantes.

---

## 13. Gestión de errores

La aplicación debe distinguir entre:

### Error de validación

Ejemplos:

- No existe `assignmentId`.
- El código está vacío.
- El lenguaje no está soportado.
- La entrega supera el tamaño máximo.

Debe responder con un error HTTP apropiado, como `400 Bad Request`.

### Error de compilación

El código fue procesado correctamente, pero no compila.

Debe almacenarse como:

```text
COMPILE_ERROR
```

No debe tratarse como un error interno del servidor.

### Error de infraestructura

Ejemplos:

- PostgreSQL no disponible.
- RabbitMQ no disponible.
- No se puede crear el contenedor.
- Se agotó el tiempo máximo.
- Error al persistir el resultado.

Debe registrarse técnicamente y producir un estado controlado, como:

```text
FAILED
```

Los mensajes no deben incluir:

- Contraseñas.
- Tokens.
- Variables de entorno sensibles.
- Rutas privadas innecesarias.
- Información interna del contenedor.

---

## 14. Pruebas requeridas

### 14.1. Pruebas del backend

Debe comprobarse que:

```bash
./gradlew clean build
```

termina correctamente desde una instalación limpia.

En Windows:

```powershell
.\gradlew.bat clean build
```

También debe funcionar:

```bash
make build
```

### 14.2. Pruebas unitarias

Como mínimo:

- Validación de una entrega vacía.
- Validación del tamaño máximo del código.
- Conversión de una solicitud a una entrega.
- Cambio correcto de estados.
- Publicación de un mensaje de evaluación.
- Conversión del resultado del sandbox.
- Código Python válido.
- Código Python inválido.
- Captura de `stdout`.
- Captura de `stderr`.
- Timeout del sandbox.
- Error del repositorio.
- Error del ejecutor.

### 14.3. Pruebas de integración

Deben utilizar PostgreSQL y RabbitMQ reales mediante contenedores o una estrategia equivalente reproducible.

Deben comprobar:

1. Se crea una asignación.
2. Se registra una entrega.
3. La entrega se persiste con estado `QUEUED`.
4. Se publica el trabajo en RabbitMQ.
5. El worker consume el trabajo.
6. El sandbox procesa el código.
7. El resultado se persiste.
8. La API devuelve el estado final.
9. Un código válido produce `COMPILED`.
10. Un código inválido produce `COMPILE_ERROR`.

### 14.4. Prueba mínima obligatoria

Debe existir una prueba equivalente a:

```text
Dado un código Python sintácticamente válido,
cuando se registra una entrega,
entonces el worker la procesa en el sandbox
y el resultado final indica COMPILED.
```

Y otra:

```text
Dado un código Python inválido,
cuando se procesa la entrega,
entonces el resultado final indica COMPILE_ERROR
y contiene el error de compilación sin exponer información sensible.
```

---

## 15. Integración continua

El workflow `.github/workflows/ci.yml` debe ejecutarse cuando:

- Se realiza un `push`.
- Se crea o actualiza un pull request.

El pipeline debe:

1. Descargar el repositorio.
2. Configurar Java 21.
3. Iniciar PostgreSQL.
4. Iniciar RabbitMQ.
5. Construir la imagen del sandbox.
6. Ejecutar las pruebas unitarias.
7. Ejecutar las pruebas de integración.
8. Compilar todos los módulos.
9. Generar los artefactos.
10. Publicar los artefactos de la ejecución.
11. Fallar si cualquiera de las etapas falla.

Comando principal:

```bash
./gradlew clean build
```

La compilación del backend y la compilación de una entrega de estudiante son responsabilidades diferentes:

```text
Backend:
./gradlew clean build

Entrega del estudiante:
python -m py_compile Main.py
```

Ambas deben estar cubiertas por pruebas, pero no deben confundirse.

---

## 16. Documentación requerida

El `README.md` debe documentar:

### Requisitos

- Java 21.
- Docker.
- Docker Compose.
- Git.
- Make opcional.
- Gradle Wrapper incluido en el repositorio.

### Instalación

```bash
git clone <URL_DEL_REPOSITORIO>
cd gradeflow
cp .env.example .env
```

En PowerShell:

```powershell
Copy-Item .env.example .env
```

### Infraestructura

```bash
make infra-up
```

### Compilación

```bash
./gradlew clean build
```

En Windows:

```powershell
.\gradlew.bat clean build
```

### Ejecución de la API

```bash
make api
```

o:

```bash
./gradlew :app-api:bootRun
```

### Ejecución del worker

```bash
make worker
```

o:

```bash
./gradlew :app-worker:bootRun
```

### Pruebas

```bash
./gradlew test
```

### Detener la infraestructura

```bash
make infra-down
```

También debe documentarse la solución de problemas relacionados con:

- Java no encontrado.
- Docker detenido.
- PostgreSQL no disponible.
- RabbitMQ no disponible.
- Puerto ocupado.
- Imagen del sandbox no construida.
- Error de compilación del backend.
- Entrega inválida.
- Tiempo de ejecución agotado.

---

## 17. Criterios de aceptación

### Backend

- [ ] El proyecto utiliza Java 21.
- [ ] El proyecto compila con Gradle Wrapper.
- [ ] `./gradlew clean build` termina correctamente.
- [ ] Se generan los artefactos de `app-api` y `app-worker`.
- [ ] La compilación funciona en GitHub Actions.

### Flujo de entregas

- [ ] Se puede registrar una entrega.
- [ ] La entrega queda persistida.
- [ ] La entrega se publica en RabbitMQ.
- [ ] El worker consume el mensaje.
- [ ] El worker ejecuta el sandbox.
- [ ] El resultado queda persistido.
- [ ] Se puede consultar el estado de la entrega.
- [ ] Se diferencia entre compilación exitosa y error de compilación.

### Sandbox

- [ ] El código no se ejecuta dentro de la API.
- [ ] El código se ejecuta dentro de un contenedor.
- [ ] El contenedor utiliza un usuario sin privilegios.
- [ ] Existe un límite de tiempo.
- [ ] Se captura la salida estándar.
- [ ] Se captura la salida de error.
- [ ] Se captura el código de salida.
- [ ] Se eliminan los archivos temporales.
- [ ] No se exponen credenciales.

### Base de datos y mensajería

- [ ] PostgreSQL se inicia mediante Docker Compose.
- [ ] RabbitMQ se inicia mediante Docker Compose.
- [ ] Flyway crea las tablas necesarias.
- [ ] Las relaciones entre asignaciones, entregas y resultados funcionan.
- [ ] Los servicios tienen healthchecks.
- [ ] Los errores de infraestructura se informan correctamente.

### Pruebas

- [ ] Existen pruebas unitarias.
- [ ] Existen pruebas de integración.
- [ ] Se prueba una entrega válida.
- [ ] Se prueba una entrega inválida.
- [ ] Se prueba un timeout.
- [ ] Se prueba la consulta del estado.
- [ ] Las pruebas se ejecutan localmente.
- [ ] Las pruebas se ejecutan en GitHub Actions.

---

## 18. Entregables

Al finalizar la Fase 1 deben existir:

```text
README.md actualizado
.env.example
Docker Compose funcional
Configuración de Gradle
Migraciones Flyway
Modelo de asignación
Modelo de entrega
Modelo de resultado de compilación
Endpoint para registrar entregas
Endpoint para consultar entregas
Publicación en RabbitMQ
Consumidor en app-worker
Ejecutor de sandbox
Imagen Docker para Python 3.12
Pruebas unitarias
Pruebas de integración
Workflow de GitHub Actions
Contrato OpenAPI actualizado
Documentación de arquitectura
```

---

## 19. Definición de terminado

La Fase 1 estará terminada cuando una persona pueda:

1. Clonar el repositorio.
2. Crear el archivo `.env`.
3. Iniciar PostgreSQL y RabbitMQ.
4. Construir la imagen del sandbox.
5. Compilar el backend con Gradle.
6. Iniciar `app-api`.
7. Iniciar `app-worker`.
8. Registrar una entrega de código.
9. Consultar el estado de la entrega.
10. Confirmar que el código se procesó en un contenedor aislado.
11. Obtener un resultado `COMPILED` para código válido.
12. Obtener un resultado `COMPILE_ERROR` para código inválido.
13. Ejecutar todas las pruebas automatizadas.

El criterio mínimo e indispensable es:

> GradeFlow debe compilar correctamente su propio backend y debe poder procesar en un sandbox aislado una entrega de estudiante, determinando si el código compila o contiene errores.