# CowTrack — Gaps and Roadmap

An honest assessment of what the system is missing, and what it would take to make it
genuinely useful to farmers of every size.

Written 26 August 2026, against commit `a99ec34`.

**How to read this.** Part 1 is what is wrong or absent *today*, with evidence from the
code. Part 2 asks who this has to serve. Part 3 onwards is what to build. Effort
estimates are rough and assume one developer.

Nothing in Part 1 is speculative — each item was verified against the source. Parts 3–5
are proposals and should be argued with.

---

## Part 1 — The nine gaps

### 1. Nothing is scoped to an owner

**Severity: critical. This is a data breach waiting for a second user.**

`GET /api/cows` returns every cow in the database. The same is true of alerts,
vaccinations, health metrics, production records and locations. Only
`FinancialRecord` and `UserPreferences` carry a `user_id`.

Two farmers signing up today would see each other's herds.

There is also no farm or organisation entity. `farmName` is a `String` column on
`User`, not something you can belong to, so there is no way to express "these three
accounts share one herd" — which is the normal case: an owner, a manager and a
caretaker.

Roles compound it. `User.Role` stores `FARMER`, `CARETAKER` and `ADMIN`, and the JWT
carries the role as a claim, but **no endpoint checks it**. There is not a single
`@PreAuthorize`, `@Secured` or `hasRole` in the codebase outside the filter chain
declaration. A caretaker can delete cattle, change financial records and read
everything.

**What to do**
- Introduce a `Farm` entity. Users belong to a farm; cattle, alerts, records and
  finances belong to a farm.
- Filter every query by the authenticated user's farm. A Hibernate filter or a
  mandatory `farmId` parameter on every repository method — the former is harder to
  forget.
- Add method-level authorisation, and a test per role asserting what it *cannot* do.
- Add an integration test that creates two farms and asserts neither can see the
  other. Until that test exists, assume the isolation is broken.

**Effort:** 3–5 days. It touches every repository, service and test.
**It gets more expensive every week.** Do this before anything else in this document.

---

### 2. The schema has no migration history

**Severity: high. Will eventually destroy data.**

`ddl-auto: update` means Hibernate generates the schema by diffing entities against
whatever is in the database. That has three consequences:

- It never drops or narrows a column, so removed fields linger forever.
- It cannot rename. A renamed field silently becomes a new column, and the old data
  stays in the abandoned one.
- The result depends on the database's prior state, so two environments that ran
  different versions end up with different schemas that both "work".

There is no migration history, no rollback and no way to review a schema change in a
pull request.

**What to do**
- Adopt Flyway. Baseline the current schema as `V1__initial.sql`, then switch to
  `ddl-auto: validate` so Hibernate verifies rather than mutates.
- Every schema change becomes a reviewable, ordered, reversible file.

**Effort:** 1 day now. Days of careful archaeology once there is production data.

---

### 3. "Real-time tracking" is not real-time

**Severity: high — it is the product's core claim.**

The pitch is live cattle tracking. There is no WebSocket support on the classpath.
`frontend/src/services/websocket.js` exists and connects to nothing;
`REACT_APP_WS_URL` points at a port with no server behind it. The Live Map performs a
single fetch on mount and never updates again.

As it stands the map is a database viewer that needs a manual refresh.

**What to do**
- Add `spring-boot-starter-websocket` with STOMP, or Server-Sent Events if only
  server-to-client push is needed — SSE is simpler and survives proxies better.
- Push location updates, new alerts and vitals to subscribed clients.
- Keep the existing fetch as the initial load and reconnect fallback.

**Effort:** 2–3 days including client reconnection handling.

---

### 4. Nothing feeds the system

**Severity: high. Without this a human is the sensor.**

There is no device API, no MQTT broker, no LoRaWAN or GSM collar integration, and no
job that pulls from hardware. Every GPS fix, temperature and heart rate has to be
POSTed by hand.

The alerting is half-wired, in a way that is easy to miss:

- Geofence breach detection **does** work — `LocationServiceImpl.recordLocation`
  calls `checkGeofenceViolations` on every new position. Good design: event-driven.
- **No-signal detection is now a scheduled sweep** (`CollarMonitoringService`,
  hourly by default). It was previously attempted inside `checkOtherAlerts`, on
  the arrival of a position, where it asked whether any position had arrived
  recently — having just been called because one had. It could not fire.

  The trap is worth naming: absence of a signal cannot be observed from the
  signal. Detecting silence requires a timer.

- **Night-movement detection still only writes to the log.** It sits in
  `checkOtherAlerts`, is correctly event-driven, and computes the distance moved
  — but never raises an alert, so `NIGHT_MOVEMENT` remains an alert type nothing
  produces.

The one scheduled job that exists, `ReminderServiceImpl.checkAndGenerateReminders`,
runs daily at 08:00 and only writes to the log. Its own comments say notifications
are "what you would do in production".

**What to do**
- Define a device ingestion API: batched, authenticated per device, tolerant of
  out-of-order and delayed readings (collars buffer when out of coverage).
- Add a scheduled sweep for silent collars and anomalous movement, and delete
  `checkOtherAlerts` or wire it in.
- Make reminders actually notify.
- Model the device itself: a collar has a battery level, a firmware version and a
  last-seen time. A flat collar battery is an operational event a farmer needs.

**Effort:** 1–2 weeks depending on hardware.

---

### 5. No pagination anywhere

**Severity: medium now, high at scale.**

Every list endpoint calls `findAll()`. There is no `Pageable` or `Page<>` in the
codebase. Fine at twelve animals; at five thousand with a year of GPS history it will
return tens of megabytes.

`/api/locations/live` is worse than a large response — it iterates every cow and
issues a separate query per animal to find the latest fix. A textbook N+1 that will
take seconds on a real herd.

`Cows.jsx` even holds pagination state (`page`, `limit`, `totalPages`) that the API
cannot satisfy, so it computes `totalPages` from the length of the array it was
given.

**What to do**
- `Pageable` on every collection endpoint; return total counts.
- Replace the live-locations loop with a single windowed query.
- Add indexes on `(cow_id, recorded_at)`, `(cow_id, record_date)` and the alert
  resolution flag.
- Consider a partitioning or retention policy for location history — it is the table
  that grows without limit.

**Effort:** 2–3 days.

---

### 6. Test coverage is shallow and one-dimensional

**Severity: medium — but it is why bugs keep surfacing late.**

29 tests. Nearly all are happy-path integration tests through `MockMvc`.

What is missing matters more than the count:

- **No unit tests for the geospatial maths.** `GeofenceCalculator` and `GeoUtils`
  decide whether an animal has left its boundary. That is the most consequential
  and least obvious code in the system, and nothing tests it. Haversine at a pole,
  across the antimeridian, at zero radius, with null coordinates — all untested.
- **No authorisation tests**, because there is nothing to test yet (see gap 1).
- **No failure-path tests** beyond the few I added while fixing 500s.
- **The frontend test suite does not run at all.** `npm test` fails before executing
  a single test: `react-router-dom` 7 is ESM-only and Create React App's Jest
  configuration cannot resolve it, so the suite errors at import.
  Result: `Test Suites: 1 failed, Tests: 0 total`.

  The one test file present is still the Create React App placeholder, asserting the
  presence of a "learn react" link that this application has never contained. So even
  if the suite loaded, it would fail. Nobody has noticed, because nothing runs it.

Everything runs against H2 in PostgreSQL-compatibility mode. That is not PostgreSQL.
Compatibility mode papers over dialect differences, so a query can pass in tests and
fail in production. Testcontainers would run the real thing.

**What to do**
- Unit-test the geometry first, with adversarial inputs.
- Move integration tests onto Testcontainers PostgreSQL.
- Add React Testing Library coverage for the auth flow and one data-driven page.

**Effort:** ongoing, but the geometry tests are half a day and the highest value.

---

### 7. Operationally blind

**Severity: medium. You would not know the app was broken.**

- No Actuator. `/api/health` is hand-rolled and always returns `UP` — it does not
  check the database, so it reports healthy while every request fails.
- No metrics, no tracing, no request IDs. A user reporting "it was slow" is
  unfalsifiable.
- `show-sql: true` and `com.cowtrack: DEBUG` are on by default. In production that
  floods the log and writes query parameters — including personal data — to disk.
- No rate limiting on `/api/auth/login`. Unlimited password attempts.
- No error tracking, so exceptions are only discoverable by reading logs.

**What to do**
- Add Actuator with real readiness and liveness probes.
- Turn SQL and debug logging off by default; enable per-environment.
- Rate-limit authentication and add lockout after repeated failures.
- Add structured JSON logging with a correlation ID per request.

**Effort:** 2 days for a large improvement.

---

### 8. Authentication is half-built

**Severity: medium.**

- **No refresh tokens.** The 24-hour access token is the entire session. Logout is
  client-side only — the token stays valid until it expires, so a stolen token
  cannot be revoked.
- **No password reset.** It needs outbound mail, which the app does not have. The
  endpoints are declared in `services/api.js` and marked unimplemented.
- **No email verification**, so addresses are unverified.
- **No lockout, no MFA, no audit trail.** Nothing records who changed what.
- The Google, Facebook and Apple buttons on the login page do nothing.

For livestock — a high-value, frequently-stolen asset — an audit trail is not a
nicety. "Who marked this animal as sold" is a question that will be asked.

**What to do**
- Short access token plus a rotating refresh token that can be revoked.
- Transactional email for reset and verification.
- An append-only audit log for every mutation of an animal or a financial record.

**Effort:** 3–4 days.

---

### 9. Money without precision discipline

**Severity: low today, painful later.**

`FinancialRecord.amount` is `BigDecimal`, which is right. But:

- There is no currency field. The UI hardcodes `R`.
- Analytics divides to compute percentages; `costBreakdown` guards a zero total but
  the rounding policy is set at the call site rather than centrally.
- No VAT handling, no financial year boundary, no reconciliation.

**What to do**
- Store currency alongside every amount.
- Introduce a `Money` type that owns rounding, or adopt a library.
- Decide the financial year explicitly rather than defaulting to calendar months.

**Effort:** 1–2 days if done before there are many records.

---

### The pattern underneath all nine

This application was built screen-first. Each page received a UI, then plausible
data, and the layer that would make it *true* — ownership, ingestion, scheduling,
migrations — was deferred.

That is why so much of it looked functional and was not: demo logins that returned
401, notifications from a hardcoded array, two logout buttons that never cleared the
session, three cattle plotted in Nairobi, and no-signal detection that could never
run.

The fix is not more screens. It is making one vertical slice true, end to end, and
then widening it.

---

## Part 2 — Who has to be served

"All types of farmers" is the hard part, because their needs diverge sharply. Five
archetypes worth designing against, roughly in order of how badly they are served
today.

### The smallholder / communal farmer

Ten to forty head, often grazed on shared communal land. Frequently the majority of
livestock owners by headcount in southern Africa.

- May not own a smartphone; may share one.
- Intermittent data, prepaid airtime, cost-sensitive per megabyte.
- Cattle are savings and status, not only income — the loss of one animal is severe.
- Animals mix with other owners' herds on shared grazing.
- Records may be oral rather than written.

**What they need:** theft alerts, a straying alert, dip and vaccination reminders,
proof of ownership. **What they do not need:** a financial dashboard.

**The app currently serves them badly.** It assumes a laptop-sized screen, a constant
connection and a literate-in-English user.

### The commercial beef producer

Two hundred to several thousand head across multiple camps.

- Rotational grazing, camp-level stocking rates, veld condition.
- Weaning weights, average daily gain, feed conversion.
- Auction and abattoir logistics; carcass grading feedback.
- Buys and sells in batches, not individuals.

**Needs:** batch operations, weight-gain analytics, camp management, market pricing.

### The dairy operator

Fifty to a thousand milking cows, twice-daily routine.

- Per-cow yield at each milking, somatic cell count, milk quality penalties.
- Lactation curves, dry-off timing, calving intervals.
- Mastitis detection, withdrawal periods after antibiotics.
- Parlour hardware that already produces data.

**Needs:** deep lactation and reproduction modelling, milk-quality tracking,
integration with existing parlour systems.

The current `ProductionRecord` — one milk figure and one weight per cow per day — is a
long way short of this. Dairy farmers record per-milking, not per-day.

### The stud / seedstock breeder

Small numbers, very high value per animal.

- Pedigree over many generations, inbreeding coefficients.
- Breed society registration, performance testing, EBVs.
- Show records, semen and embryo inventory.
- Buyers demand documented provenance.

**Needs:** genuine pedigree modelling. The current `mother`/`father` self-references
are a start but there is no ancestry traversal, no breeding value, no registration.

### The feedlot

Thousands of animals on short cycles.

- Pen-level rather than animal-level management.
- Feed cost per kilogram of gain is the entire business.
- Mortality and morbidity rates, treatment protocols.
- Tight margins measured daily.

**Needs:** pen-based grouping, ration costing, throughput analytics.

### The uncomfortable conclusion

**No single UI serves all five.** A dairy farmer opening a beef-oriented dashboard
sees noise; a smallholder opening either sees an aeroplane cockpit.

The realistic answer is a shared core — animals, movement, health, events — with
role-based and enterprise-based views layered on top. Choose the farm type at setup
and let it drive which modules appear. That is an architectural decision to make
early, because retrofitting it is expensive.

---

## Part 3 — What would make this genuinely impressive

### 3.1 Theft prevention and recovery

In southern Africa this is arguably the highest-value feature that exists, and the
app is already most of the way to it.

- **Instant breach alerts** with escalation: push, then SMS, then a phone call.
- **Tamper detection** — a collar removed, cut or shielded. The `DEVICE_REMOVED`
  alert type already exists and nothing raises it.
- **Movement-pattern anomaly detection.** Cattle move at a walking pace and rest at
  night. Movement at 40 km/h, or a straight line down a road at 02:00, is a vehicle.
- **Silent alarm and live pursuit mode** — a shareable live track for police or a
  farm-watch group.
- **Neighbour network.** Adjacent farms opt in to be notified when an animal strays
  onto their land. Communal grazing makes this essential rather than optional.
- **Proof of ownership**: photographs, brand and tattoo marks, an immutable
  ownership history. In South Africa the Animal Identification Act requires a
  registered mark; the app should store and produce it. *(Verify current legal
  requirements before relying on this.)*

### 3.2 Health that predicts rather than reports

Current health tracking is a record of the past. The valuable version anticipates.

- **Individual baselines.** A resting heart rate of 72 is alarming for one animal and
  normal for another. Fixed thresholds — as `HealthMetric` uses now — generate false
  positives and miss real illness.
- **Early illness detection** from combined signals: activity drop plus temperature
  rise plus reduced rumination, twenty-four to seventy-two hours before visible
  symptoms. This is where the commercial value in collars actually lies.
- **Heat (oestrus) detection** from activity spikes. For a dairy or breeding
  operation, a missed heat costs roughly a lost cycle of production. This alone
  justifies collars for many farmers.
- **Calving prediction** from restlessness and isolation behaviour, so a farmer can
  attend a difficult birth.
- **Withdrawal period tracking.** After antibiotics, milk and meat must be withheld
  for a set period. Getting this wrong contaminates a bulk tank and is a regulatory
  offence. The app should make selling or milking a withheld animal *impossible*,
  not merely warned against.
- **Mortality recording** with cause, because the pattern matters.

### 3.3 Reproduction and the herd calendar

Largely absent today and central to every livestock enterprise.

- Service and insemination records, sire selection, pregnancy diagnosis.
- Expected calving dates and a calving book.
- Calving interval, conception rate, days open — the metrics that decide whether a
  herd is profitable.
- Weaning records tied to the dam, so cow performance is measurable.
- Culling recommendations from age, fertility and production history.

### 3.4 Grazing, land and feed

The animals are half the system; the land is the other half.

- **Camp and paddock mapping** with area, and stocking rate against carrying
  capacity.
- **Rotational grazing planner** with rest periods and a movement history per camp.
- **Veld condition**, ideally from satellite NDVI, which is freely available.
- **Feed inventory and ration costing** — cost per kilogram of gain is the number a
  feedlot lives by.
- **Water point monitoring.** In a dry region a failed pump kills animals within
  days.
- **Rainfall records** correlated with grazing and production.

### 3.5 Money that reflects the farm

The current financial model — a flat list of revenue and cost lines — is too coarse.

- **Cost per animal** and per kilogram produced.
- **Enterprise budgeting**: compare a planned season against the actual one.
- **Break-even analysis** per animal class.
- **Market prices** from auctions and abattoirs, so "sell now or feed longer" is an
  informed decision.
- **Asset register and depreciation** for herd valuation, which is what a bank asks
  for.
- **Loan and insurance records**, since livestock is often collateral.

### 3.6 Compliance and traceability

Unglamorous, and it decides whether a farmer can sell at all.

- **Full traceability** from birth to sale, which export markets require.
- **Movement permits** and disease-control zone awareness. Foot-and-mouth zones in
  particular restrict movement. *(Requirements vary and change; verify.)*
- **Treatment register** satisfying veterinary record-keeping obligations.
- **Dip tank attendance**, which is a statutory requirement in some regions.
- **Auction and abattoir documentation** generated rather than hand-written.
- **Certification support** — organic, free-range, grass-fed — where each depends on
  provable records.

### 3.7 Reaching farmers who are not at a desk

This is where the current app fails hardest, and where most farmers actually are.

- **Offline-first mobile.** A farmer walking camps has no signal. The app must record
  locally and reconcile later. This is an architectural choice, not a feature — it
  cannot be bolted on.
- **USSD and SMS.** A farmer with a feature phone should be able to check an animal
  or receive a theft alert. USSD works on every handset and needs no data.
- **Voice notes** instead of typing, for users who are more comfortable speaking.
- **Local languages.** In South Africa alone that means at minimum isiZulu, Sesotho,
  Setswana, Afrikaans, isiXhosa and Sepedi. The app is English-only, and
  `UserPreferences.language` is stored but unused.
- **Low-bandwidth mode.** The current bundle is 286 KB gzipped (1.0 MB raw) before
  a single map tile loads.
  On prepaid data that is real money.
- **Load-shedding tolerance.** Scheduled power cuts are routine; the app must degrade
  gracefully rather than fail.
- **Photograph-based identification** — recognising an animal from a phone photo —
  for herds with no collars at all.

### 3.8 Working with other people

Farming is not solitary, and the app currently assumes one user.

- **Multi-user farms with real roles**: owner, manager, worker, vet, accountant.
- **Task assignment** with completion tracking, so a manager can direct work.
- **Vet portal** — a vet treating animals across many farms should not need an
  account per farm.
- **Cooperative and group features**: shared grazing, collective marketing, group
  purchasing.
- **Extension officer view** for the government and NGO staff who support
  smallholders.
- **Buyer and auction integration**, so a listing carries its records with it.

### 3.9 Intelligence worth the name

Only after the data is real. Marketing claims of "AI-powered analytics" on a system
with no ingestion are exactly the kind of thing this document exists to prevent.

- Yield forecasting from history, breed and season.
- Optimal sale timing from weight-gain curves against market prices.
- Genetic pairing suggestions from performance data.
- Disease-outbreak risk from regional patterns.
- Natural-language querying: "which cows have not calved this year?"

---

## Part 4 — Technical foundations

Ordered by how much later pain each prevents.

| Area | Now | Should be |
|---|---|---|
| Tenancy | Global queries | Farm-scoped, enforced centrally |
| Schema | `ddl-auto: update` | Flyway migrations, `validate` |
| Live data | One-shot fetch | WebSocket/SSE push |
| Ingestion | Manual POST | Device API, batched and offline-tolerant |
| Lists | `findAll()` | Paginated and indexed |
| Tests | 29 happy-path | Unit + authorisation + Testcontainers |
| Observability | None | Actuator, metrics, structured logs, tracing |
| Auth | 24h JWT, no revoke | Refresh tokens, audit trail, MFA |
| Mobile | Responsive web | Offline-first native or PWA |
| i18n | English only | Multilingual, RTL-safe |
| Deployment | Local jar | Containerised, CI/CD, staged environments |
| Backups | None | Automated, tested restores |

Two additional items with no current presence at all:

- **There is no CI.** No pipeline runs anything — no `.github/workflows`, no
  `.gitlab-ci.yml`. The backend tests pass because I ran them by hand. The frontend
  suite has been broken for long enough that nobody knows, which is precisely what
  the absence of CI buys you.
- **There is no backup story.** For a farmer whose herd records are their asset
  register, losing the database is losing proof of ownership.

---

## Part 5 — Sequencing

Nine gaps and nine capability areas is not a plan. This is.

### Phase 0 — Make it safe (1–2 weeks)

Non-negotiable, and blocking everything else.

1. Farm-scoped tenancy and role enforcement (gap 1)
2. Flyway migrations (gap 2)
3. Actuator, real health checks, sane logging defaults (gap 7)
4. Login rate limiting (gap 8)
5. CI running the test suite
6. Unit tests for the geofence maths (gap 6)

### Phase 1 — Make it true (2–4 weeks)

Make one vertical slice real end to end.

1. Device ingestion API, tolerant of delay and duplication (gap 4)
2. Scheduled sweeps for silent collars and anomalies; delete the dead code
3. Real-time push (gap 3)
4. Pagination and indexing (gap 5)
5. Notifications that actually deliver — push, then SMS

Pick **theft detection** as the slice. It is the highest-value use case, it exercises
ingestion, alerting, real-time and notification together, and it is nearly built.

### Phase 2 — Make it reach (4–8 weeks)

1. Offline-first mobile client
2. USSD and SMS for feature phones
3. Multilingual support
4. Multi-user farms with roles and task assignment

### Phase 3 — Make it deep (ongoing)

Choose one enterprise type and go deep rather than broad across all five.

Given the setting, **beef and communal smallholders** are the larger underserved
market; dairy is better served by existing commercial software with parlour
integrations that are hard to displace.

1. Reproduction and the herd calendar
2. Grazing and camp management
3. Compliance and traceability
4. Financial depth
5. Predictive health

---

## What not to build

Worth stating, because the temptation is real.

- **Do not add more dashboards.** There are already more charts than there is data.
- **Do not build blockchain traceability.** A signed audit log solves the same problem
  and farmers can actually use it.
- **Do not promise AI before ingestion exists.** The landing copy already claims
  "AI-powered analytics" for a system where every reading is typed in by hand.
- **Do not build a marketplace early.** It is a two-sided problem, an order of
  magnitude harder than the tracking product, and it will consume everything.
- **Do not chase all five farmer archetypes at once.** Serving one properly beats
  serving five partially.

---

## The single most important thing

If only one item from this document is ever acted on: **farm-scoped tenancy**
(gap 1).

It is a live security hole, it sits in the data model where changes are most
expensive, and it is the only item that becomes structurally harder to fix with every
feature added. Everything else in this document can wait. That cannot.
