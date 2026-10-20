# GradeFlow — Guía de integración para frontend (Fase 1)

Todo lo necesario para construir un frontend que consuma el backend de la Fase 1. El backend expone una API REST JSON sobre HTTP. No hay autenticación en esta fase.

> Fuente de verdad del contrato: [`api-contract/openapi.yaml`](../api-contract/openapi.yaml). Puedes generar un cliente tipado con cualquier generador OpenAPI 3.

---

## 1. Configuración base

| Dato | Valor por defecto |
|---|---|
| Base URL | `http://localhost:8080` |
| Prefijo de la API | `/api/v1` |
| Content-Type | `application/json` |
| Autenticación | Ninguna (Fase 1) |
| Puerto | `SERVER_PORT` (8080 por defecto) |

El backend debe estar arrancado (`make api`) junto a PostgreSQL y RabbitMQ (`make infra-up`) y el worker (`make worker`). Ver [README](../README.md).

**Asignación sembrada:** la migración Flyway crea una asignación demo de Python con id fijo:
```
11111111-1111-1111-1111-111111111111
```
Úsala para enviar entregas. En Fase 1 **no hay endpoints para crear ni listar asignaciones** (fuera de alcance), así que el frontend trabaja contra este id conocido.

---

## 2. Flujo que implementa el frontend

```
1. POST /api/v1/assignments/{assignmentId}/submissions   -> recibe submissionId + QUEUED
2. Hacer polling de GET /api/v1/submissions/{submissionId} hasta un estado final
3. Mostrar el resultado (COMPILED / COMPILE_ERROR / FAILED)
```

El procesamiento es **asíncrono**: el POST sólo encola. El resultado aparece más tarde al consultar.

---

## 3. Endpoints

### 3.1. Registrar una entrega

```
POST /api/v1/assignments/{assignmentId}/submissions
Content-Type: application/json
```

Cuerpo:
```json
{ "sourceCode": "print('Hola GradeFlow')" }
```

Respuesta `202 Accepted`:
```json
{ "submissionId": "e2b1...uuid", "status": "QUEUED" }
```

Errores:
| Código | Cuándo | Cuerpo |
|---|---|---|
| `400 Bad Request` | código vacío, demasiado grande (> 1 MB), o lenguaje no soportado | `{ "error": "..." }` |
| `404 Not Found` | el `assignmentId` no existe | `{ "error": "Assignment not found" }` |

### 3.2. Consultar el estado de una entrega

```
GET /api/v1/submissions/{submissionId}
```

Respuesta `200 OK` mientras se procesa (sin `result`):
```json
{ "submissionId": "e2b1...uuid", "status": "RUNNING", "result": null }
```

Respuesta `200 OK` tras el procesamiento:
```json
{
  "submissionId": "e2b1...uuid",
  "status": "COMPILED",
  "result": {
    "exitCode": 0,
    "stdout": "",
    "stderr": "",
    "durationMilliseconds": 120
  }
}
```

Error de compilación:
```json
{
  "submissionId": "e2b1...uuid",
  "status": "COMPILE_ERROR",
  "result": { "exitCode": 1, "stdout": "", "stderr": "SyntaxError: invalid syntax", "durationMilliseconds": 95 }
}
```

`404 Not Found` si el `submissionId` no existe.

---

## 4. Ciclo de vida del estado

```
QUEUED ──> RUNNING ──> COMPILED        (exitCode 0, el código compila)
                   ├──> COMPILE_ERROR  (el código tiene errores de sintaxis; stderr los describe)
                   └──> FAILED          (fallo de infraestructura o timeout; no es culpa del código)
```

| Estado | ¿Final? | Significado para la UI |
|---|---|---|
| `QUEUED` | no | En cola, aún no procesada |
| `RUNNING` | no | El worker la está compilando |
| `COMPILED` | sí | Compiló correctamente (verde) |
| `COMPILE_ERROR` | sí | No compila; muestra `result.stderr` (ámbar) |
| `FAILED` | sí | Error del sistema/timeout; invita a reintentar (rojo) |

`result` es `null` hasta llegar a un estado final.

---

## 5. Patrón de polling recomendado

No hay websockets ni SSE en Fase 1. Haz polling del GET cada 1–2 s hasta un estado final, con un tope de reintentos.

```js
const BASE = "http://localhost:8080/api/v1";
const ASSIGNMENT = "11111111-1111-1111-1111-111111111111";
const FINAL = ["COMPILED", "COMPILE_ERROR", "FAILED"];

async function submitAndWait(sourceCode) {
  const res = await fetch(`${BASE}/assignments/${ASSIGNMENT}/submissions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sourceCode }),
  });
  if (!res.ok) throw new Error((await res.json()).error ?? res.statusText);
  const { submissionId } = await res.json();

  for (let i = 0; i < 30; i++) {            // ~30 s máx
    const view = await (await fetch(`${BASE}/submissions/${submissionId}`)).json();
    if (FINAL.includes(view.status)) return view;
    await new Promise((r) => setTimeout(r, 1000));
  }
  throw new Error("Timed out waiting for result");
}
```

---

## 6. Manejo de errores

- Los errores de la API devuelven un cuerpo JSON uniforme: `{ "error": "mensaje" }`. No exponen detalles internos.
- `COMPILE_ERROR` **no es** un error HTTP: el GET responde `200` con `status: "COMPILE_ERROR"`. Trátalo como un resultado válido, no como fallo de red.
- Distingue en la UI: `400/404` (entrada del usuario) vs `FAILED` (sistema) vs `COMPILE_ERROR` (código del estudiante).

---

## 7. CORS — el único cambio de backend que necesitarás

El backend actual **no habilita CORS**. Un frontend servido desde otro origen (p. ej. `http://localhost:5173` con Vite) será bloqueado por el navegador. Opciones:

- **Dev:** usa el proxy de tu dev server (Vite/Next reenvían `/api` a `localhost:8080`), y no necesitas tocar el backend.
- **Habilitar CORS en el backend:** añade un bean en `app-api` (origen explícito, nunca `*` con credenciales):

```java
@Configuration
class CorsConfig implements WebMvcConfigurer {
    @Override public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/api/**").allowedOrigins("http://localhost:5173").allowedMethods("GET", "POST");
    }
}
```

> Esto no está incluido en Fase 1 porque todavía no existe frontend; decídelo según dónde lo despliegues.

---

## 8. Fuera de alcance en Fase 1 (para que el frontend no lo espere)

No existen todavía: login/usuarios/roles, crear o listar asignaciones, listar entregas, reporte calificado (`/report`), múltiples lenguajes (sólo Python), subir archivos (el código va como texto en `sourceCode`). El frontend de Fase 1 se limita a: enviar código Python contra la asignación demo y mostrar el resultado de compilación.
