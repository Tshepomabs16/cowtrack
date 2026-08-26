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

`backend` is the build root: its `mvn package` builds `frontend` and bundles the
output, producing one jar that serves both.

## Stack

| | Backend | Frontend |
|---|---|---|
| Language | Java 17 | JavaScript (ES2020+) |
| Framework | Spring Boot 3.5.9 | React 19 + React Router 7 |
| Build | Maven (`mvnw`) | react-scripts 5 |
| Persistence | Spring Data JPA → PostgreSQL 16 | — |
| UI | — | MUI 7, Emotion |
| Mapping | JTS (geometry/geofencing) | Leaflet + react-leaflet, leaflet-draw |
| Charts | — | Recharts |
| HTTP | Spring Web | Axios |
| Security | Spring Security | Context-based auth guard (`ProtectedRoute`) |

## Domain model

The core entities live in `backend/src/main/java/com/cowtrack/entity/`:

- **Cow** — the tracked animal, including breed
- **LocationRecord** — timestamped GPS positions
- **Geofence** — boundary polygons; breaches are evaluated by `GeofenceCalculator`
- **Alert** — raised on geofence breach and other conditions
- **HealthRecord** — veterinary diagnosis and treatment history
- **HealthMetric** — collar vitals (temperature, heart rate, activity)
- **Vaccination** — administered and scheduled doses
- **ProductionRecord** — daily milk yield and weight
- **FinancialRecord** — revenue and cost lines
- **Reminder** — scheduled husbandry tasks
- **User** / **UserPreferences** — account, profile and display settings

### Derived fields

Some values the UI filters on are computed rather than stored, so they cannot drift
out of step with the data they summarise:

| Field | Derived from |
|---|---|
| `AlertResponse.severity` | `alertType` — breach and collar removal are critical |
| `AlertResponse.title` | `alertType`, as a readable heading |
| `CowResponse.status` | active alerts, collar freshness, and vitals; a warning reading always wins |
| `CowResponse.age` | `dateOfBirth` |
| `CowResponse.temperature` / `heartRate` / `lastCheck` | most recent `HealthMetric` |
| `Vaccination.status` | `administeredDate` and `nextDueDate` |

## Running it

The project builds to **a single jar** that serves both the API and the web client
from one port. `mvn package` compiles the React app and bundles it into the jar as
static content, so there is nothing separate to deploy.

### Prerequisites

- JDK 17+
- PostgreSQL 14+ with a `cowtrack` database — *or* use the `dev` profile below,
  which needs no database at all

Node is **not** required: the build downloads its own pinned copy.

### One command

```bash
cd backend
./mvnw clean package
java -jar target/cowtrack-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Open <http://localhost:8081>. The `dev` profile uses an in-memory H2 database, so
you can register an account and click around immediately; data is discarded on exit.
Drop `--spring.profiles.active=dev` to run against PostgreSQL, supplying the
password through the environment:

```bash
createdb cowtrack
export DB_PASSWORD='...'
java -jar target/cowtrack-backend-0.0.1-SNAPSHOT.jar
```

The password is deliberately **not** stored in `application.yml` — this repository
is public.

### Working on the frontend

For hot reload, run the CRA dev server alongside the backend:

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # terminal 1
cd frontend && npm install && npm start                               # terminal 2
```

The dev server on <http://localhost:3000> proxies `/api` to port 8081 (see `proxy`
in `package.json`), so the client talks to a same-origin `/api` in both modes and
CORS never comes into play.

### Backend-only builds

`./mvnw package -DskipFrontend=true` skips the React build entirely — much faster
when you are only touching Java.

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
| `DB_HOST` / `DB_PORT` / `DB_NAME` | PostgreSQL connection | `localhost` / `5432` / `cowtrack` |
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL credentials | `postgres` / *(empty)* |
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
cd backend && ./mvnw test -DskipFrontend=true
```

Tests run against an in-memory H2 database via the `test` profile, so no PostgreSQL
instance is needed. `AuthIntegrationTest` covers the login flow end to end;
`SpaRoutingTest` guards the boundary between client routes and API paths.

## API response envelope

Successful responses are wrapped:

```json
{ "success": true, "message": "...", "data": { }, "timestamp": "..." }
```

The axios interceptor in `services/api.js` unwraps this, so `response.data` is always
the payload itself. The auth endpoints are the one exception — they return
`{ token, user }` unwrapped, because that is the shape `AuthContext` reads.

`ApiContractTest` walks every path declared in `services/api.js` and asserts the
backend answers it, so a client call cannot silently start 404ing.

## Where this is going

[ROADMAP.md](ROADMAP.md) records what the system is missing and what it would take to
serve farmers of every size. The nine gaps in Part 1 were each verified against the
source; the most serious is that **no query is scoped to an owner**, so a second
farmer signing up would see the first one's herd.

## Known issues

- `REACT_APP_WS_URL` has no server behind it — the backend has no WebSocket starter
  on the classpath, so `services/websocket.js` will never connect. The Live Map falls
  back to the initial fetch.
- `authAPI.forgotPassword` and `resetPassword` are **not implemented**: password reset
  needs an outbound mail service the application does not have. They are marked as
  such in `services/api.js` and will 404.
- Feeding is not modelled: there is no feed schedule or intake entity, so the
  Feeding tab on a cow shows an empty state rather than invented figures.
- There is still no **UI** for entering production or financial records. The
  endpoints exist (`POST /api/production`, `POST /api/financials`) but data has to
  be posted directly for now.
- The application connects as the `postgres` superuser. A role scoped to just the
  `cowtrack` database would be a better fit before this is deployed anywhere.

## Author

Tshepo Maabane — [@Tshepomabs16](https://github.com/Tshepomabs16)
