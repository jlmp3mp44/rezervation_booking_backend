# HOROVOD HUB — Spring Boot Backend

Java 21 + Spring Boot 3.4 backend for the rehearsal booking app.

## Architecture

```
index.html + style.css + app.js   (unchanged UI/logic)
        ↓
backend-shim.js                   (Supabase-compatible API adapter)
        ↓
Spring Boot REST API (:8080)      (business rules, persistence, auth)
        ↓
H2 (dev) / PostgreSQL (prod)
```

Business logic (booking validation, slot conflicts, auto-expire pending, log trimming) lives in Java services under `backend/src/main/java/com/horovod/hub/service/`.

## Requirements

- **JDK 21** (required by `pom.xml`)
- Maven 3.9+

## Quick start

### 1. Start backend

```bash
cd backend
mvn spring-boot:run
```

API: http://localhost:8080/api  
H2 console (dev): http://localhost:8080/h2-console

### 2. Start frontend

```bash
# from project root
python -m http.server 3000
```

Open http://localhost:3000

### 3. Login (test)

| Role | Email | Credentials |
|------|-------|-------------|
| Admin | horovod.info@gmail.com | password `21admin02` |
| Resident | any@email.com | OTP `0000` or `1234` |

OTP codes are also printed to the backend console on send.

## PostgreSQL (optional)

```bash
cd backend
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Production API URL

Set before loading the shim (in `index.html` or hosting inject):

```html
<script>window.HOROVOD_API_URL = 'https://api.horovod.sk/api';</script>
```

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/bookings` | All bookings |
| POST | `/api/bookings` | Create booking (validated) |
| PUT | `/api/bookings/{id}` | Update booking |
| POST | `/api/bookings/upsert` | Upsert one or many |
| GET | `/api/logs` | Recent logs (max 50) |
| POST | `/api/logs` | Add log entry |
| GET | `/api/issues` | All issues |
| POST | `/api/issues` | Report issue |
| PUT | `/api/issues/{id}` | Update issue |
| DELETE | `/api/issues/{id}` | Delete issue |
| GET | `/api/auth/session` | Current session |
| POST | `/api/auth/login/password` | Admin login |
| POST | `/api/auth/otp/send` | Send OTP |
| POST | `/api/auth/otp/verify` | Verify OTP |
| GET | `/api/events/stream` | SSE realtime sync |

## Configuration (`application.yml`)

| Key | Default | Description |
|-----|---------|-------------|
| `horovod.admin-email` | horovod.info@gmail.com | Admin account |
| `horovod.admin-password-fallback` | 21admin02 | Offline admin password |
| `horovod.otp-bypass-codes` | 0000,1234 | Dev/test OTP codes |
| `horovod.max-concurrent-per-slot` | 2 | Slot capacity |
| `horovod.email-enabled` | false | Email sending |

## What stayed unchanged

- `app.js` — no edits
- `style.css` — no edits
- `index.html` — only Supabase CDN replaced with `backend-shim.js` (2 lines)

## Tests

```bash
cd backend
mvn test
```

Frontend Selenium tests still work: `navigator.webdriver === true` disables cloud in `app.js` (localStorage-only mode).
