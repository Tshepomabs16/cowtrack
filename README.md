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

## Configuration

`frontend/.env` holds the client-side settings:

| Variable | Purpose | Default |
|---|---|---|
| `REACT_APP_API_URL` | Backend REST base URL | `http://localhost:8081/api` |
| `REACT_APP_WS_URL` | WebSocket endpoint | `ws://localhost:3000` |
| `REACT_APP_MAPBOX_TOKEN` | Mapbox access token | *currently unused* |
| `REACT_APP_GOOGLE_MAPS_KEY` | Google Maps API key | *currently unused* |

Note that any `REACT_APP_*` value is compiled into the production bundle and is
therefore **public** — never put a secret that needs to stay private in this file.

## Known issues

- `backend/src/main/resources/application.yml` nests a second `spring:` block inside
  the outer `spring:` key. Everything under it (the HikariCP pool sizing and
  `open-in-view: false`) resolves to `spring.spring.*` and is silently ignored by
  Spring Boot. The nested keys need to be lifted one level up, and `Hikari` should be
  lowercase `hikari`.
- `REACT_APP_WS_URL` points at port 3000, the frontend's own dev server. The backend
  has no WebSocket starter on the classpath, so no WebSocket endpoint exists yet.
- Build output (`backend/target/`, 85 compiled `.class` files) and IDE settings
  (`.idea/` in both halves) are tracked in git and should be removed and gitignored.

## Author

Tshepo Maabane — [@Tshepomabs16](https://github.com/Tshepomabs16)
