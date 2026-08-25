# CowTrack

Livestock tracking and management platform. Monorepo containing the Spring Boot API and the React web client.

This repository was formed by merging two previously separate repositories
([cowtrack-backend](https://github.com/Tshepomabs16/cowtrack-backend) and
[cowtrack-frontend](https://github.com/Tshepomabs16/cowtrack-frontend)) with their
commit histories preserved under the `backend/` and `frontend/` paths.

## Layout

```
cowtrack/
├── backend/    Spring Boot 3.5.9 REST API (Java 17, Maven)
└── frontend/   React 19 single-page app (Create React App)
```

## Stack

| | Backend | Frontend |
|---|---|---|
| Language | Java 17 | JavaScript (ES2020+) |
| Framework | Spring Boot 3.5.9 | React 19 + React Router 7 |
| Build | Maven (`mvnw`) | react-scripts 5 |
| Persistence | Spring Data JPA → MySQL 8 | — |
| UI | — | MUI 7, Emotion |
| Mapping | JTS (geometry/geofencing) | Leaflet + react-leaflet, leaflet-draw |
| Charts | — | Recharts |
| HTTP | Spring Web | Axios |
| Security | Spring Security | Context-based auth guard (`ProtectedRoute`) |

## Domain model

The core entities live in `backend/src/main/java/com/cowtrack/entity/`:

- **Cow** — the tracked animal
- **LocationRecord** — timestamped GPS positions
- **Geofence** — boundary polygons; breaches are evaluated by `GeofenceCalculator`
- **Alert** — raised on geofence breach and other conditions
- **HealthRecord** — veterinary and health history
- **Reminder** — scheduled husbandry tasks
- **User** — account and ownership

## Running locally

Both halves run separately. Start the backend first — the frontend expects it on port 8081.

### Prerequisites

- JDK 17+
- Node.js 18+ and npm
- MySQL 8 listening on **port 3307** with a database named `cowtrack`

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Serves on <http://localhost:8081>. Datasource settings are in
`backend/src/main/resources/application.yml`. Hibernate runs with `ddl-auto: update`,
so the schema is created and migrated automatically on first start.

### Frontend

```bash
cd frontend
npm install
npm start
```

Serves on <http://localhost:3000> and proxies API calls to `REACT_APP_API_URL`.

> The per-project README in `frontend/` says `npm run dev` — that script does not
> exist. This is a Create React App project, so the dev server is `npm start`.

## Authentication

The API uses stateless JWT bearer tokens.

1. `POST /api/auth/register` or `POST /api/auth/login` returns `{ token, user }`.
2. The client stores the token and sends `Authorization: Bearer <token>` on every
   subsequent request (handled by the axios interceptor in `services/api.js`).
3. Everything except `/api/auth/**`, `/`, `/api/health` and `/api/ping` requires a
   valid token, and returns **401** without one.

## Configuration

### Backend

All settings have working defaults for local development; override via environment
variables for anything deployed.

| Variable | Purpose | Default |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL connection | `localhost` / `3307` / `cowtrack` |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL credentials | `root` / *(empty)* |
| `JWT_SECRET` | Token signing key, **min 32 chars** | insecure dev default |
| `JWT_EXPIRATION_MS` | Token lifetime | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Origins allowed to call the API | `http://localhost:3000` |

`JWT_SECRET` **must** be overridden outside local development. Generate one with
`openssl rand -base64 48`.

### Frontend

See `frontend/.env.example`. Every `REACT_APP_*` value is compiled into the production
bundle and is therefore **public** — never put a private secret in that file.

## Testing

```bash
cd backend && ./mvnw test
```

Tests run against an in-memory H2 database via the `test` profile, so no MySQL
instance is needed. `AuthIntegrationTest` covers the full login flow end to end.

## Known issues

- `REACT_APP_WS_URL` has no server behind it — the backend has no WebSocket starter
  on the classpath, so `services/websocket.js` will never connect. The Live Map falls
  back to the initial fetch.
- **The frontend and backend APIs still largely disagree.** `services/api.js` declares
  endpoints (`/settings/*`, `/upload/*`, `/analytics/*`, `/locations/live`) that the
  backend does not implement. Auth is reconciled; the rest is not.
- Six pages still render hardcoded mock data rather than live API results:
  Dashboard, Analytics, CowDetail, Health, Reminders, Settings.

## Author

Tshepo Maabane — [@Tshepomabs16](https://github.com/Tshepomabs16)
