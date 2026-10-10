package com.cowtrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.cowtrack.realtime.SseHub;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The push channel decides who sees what without going near the database, so
 * none of the farm scoping in the repositories applies to it. That makes it a
 * tenancy boundary of its own, and one that a mocked hub would not really test:
 * these run against a real server over real event-stream connections, because
 * the thing worth proving is that a farmer watching their own map never sees
 * another farm's cattle appear on it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
// A client that goes away is only noticed when the next write to it fails, so
// the heartbeat interval is also how long a dead subscriber lingers. At the
// 25-second default the connections these tests open outlive the tests, and the
// server cannot shut down until they are reaped.
@TestPropertySource(properties = "cowtrack.realtime.heartbeat-interval=300ms")
class RealtimePushTest {

    @LocalServerPort private int port;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SseHub hub;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<Thread> readers = new ArrayList<>();
    private final List<java.util.stream.Stream<String>> bodies = new ArrayList<>();

    private String tokenA;
    private String tokenB;
    private Long cowA;

    @BeforeEach
    void setUp() throws Exception {
        tokenA = register("Alpha");
        tokenB = register("Beta");
        cowA = createCow(tokenA, "RT-A");
    }

    /**
     * Closing the response bodies is what actually ends these connections. Left
     * open they stay registered on the server for the full stream timeout, and
     * the context cannot shut down until they go, which stalls the build.
     */
    @AfterEach
    void tearDown() {
        bodies.forEach(body -> {
            try {
                body.close();
            } catch (Exception ignored) {
                // Already ended with the connection.
            }
        });
        bodies.clear();

        readers.forEach(Thread::interrupt);
        readers.clear();
    }

    // ---------------------------------------------------------------- tests

    @Test
    void aSubscriberIsToldTheStreamIsLive() throws Exception {
        BlockingQueue<JsonNode> stream = openStream(ticketFor(tokenA));

        JsonNode connected = awaitEvent(stream, "connected", Duration.ofSeconds(5));

        assertThat(connected).as("the stream should announce itself").isNotNull();
    }

    @Test
    void aPositionReachesTheFarmThatRecordedIt() throws Exception {
        BlockingQueue<JsonNode> stream = openStream(ticketFor(tokenA));
        awaitEvent(stream, "connected", Duration.ofSeconds(5));

        recordPosition(tokenA, cowA, "-23.9045", "29.4689");

        JsonNode update = awaitEvent(stream, "location_update", Duration.ofSeconds(5));
        assertThat(update).as("the farm should be pushed its own position").isNotNull();
        assertThat(update.get("payload").get("cowId").asLong()).isEqualTo(cowA);
        assertThat(update.get("payload").get("latitude").asDouble()).isEqualTo(-23.9045);
    }

    /**
     * The whole point of keying the registry by farm. Farm B is subscribed and
     * idle while farm A moves cattle and trips a geofence; B must see none of it.
     */
    @Test
    void oneFarmsActivityNeverReachesAnothersStream() throws Exception {
        BlockingQueue<JsonNode> streamA = openStream(ticketFor(tokenA));
        BlockingQueue<JsonNode> streamB = openStream(ticketFor(tokenB));
        awaitEvent(streamA, "connected", Duration.ofSeconds(5));
        awaitEvent(streamB, "connected", Duration.ofSeconds(5));

        createGeofence(tokenA, cowA, "-23.9045", "29.4689", 100);
        recordPosition(tokenA, cowA, "-23.9045", "29.4689");
        recordPosition(tokenA, cowA, "-23.9500", "29.5200");

        // Wait on A first: both streams are written in the same fan-out, so once
        // A has the event, B has had its chance at it.
        assertThat(awaitEvent(streamA, "new_alert", Duration.ofSeconds(5)))
                .as("farm A should be told its own animal left its geofence")
                .isNotNull();

        List<JsonNode> leaked = drain(streamB, Duration.ofMillis(500));

        assertThat(leaked)
                .as("farm B subscribed and did nothing; every event here is another farm's")
                .isEmpty();
    }

    @Test
    void aGeofenceBreachIsPushedAsWellAsStored() throws Exception {
        BlockingQueue<JsonNode> stream = openStream(ticketFor(tokenA));
        awaitEvent(stream, "connected", Duration.ofSeconds(5));

        createGeofence(tokenA, cowA, "-23.9045", "29.4689", 100);
        recordPosition(tokenA, cowA, "-23.9045", "29.4689");
        recordPosition(tokenA, cowA, "-23.9500", "29.5200");

        JsonNode alert = awaitEvent(stream, "new_alert", Duration.ofSeconds(5));

        assertThat(alert).isNotNull();
        assertThat(alert.get("payload").get("alertType").asText()).isEqualTo("GEOFENCE_BREACH");
        assertThat(alert.get("payload").get("severity").asText()).isEqualTo("critical");
        assertThat(alert.get("payload").get("cowId").asLong()).isEqualTo(cowA);
    }

    @Test
    void aTicketCannotOpenASecondStream() throws Exception {
        String ticket = ticketFor(tokenA);
        openStream(ticket);

        HttpResponse<String> replay = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/realtime/stream?ticket=" + ticket)))
                        .header("Accept", "text/event-stream")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(replay.statusCode()).isEqualTo(401);
    }

    @Test
    void theStreamCannotBeOpenedWithoutATicket() throws Exception {
        HttpResponse<String> forged = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/realtime/stream?ticket=made-up")))
                        .header("Accept", "text/event-stream")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(forged.statusCode()).isEqualTo(401);
    }

    /**
     * Clients reconnect constantly — every stream timeout, every lost signal, every
     * page reload — so an emitter that outlives its connection is not a leak that
     * stays small. Nothing writes to a stream on a quiet farm, so the heartbeat is
     * what discovers the peer is gone.
     */
    @Test
    void aDepartedSubscriberIsDroppedRatherThanAccumulating() throws Exception {
        int before = hub.totalSubscribers();

        BlockingQueue<JsonNode> stream = openStream(ticketFor(tokenA));
        awaitEvent(stream, "connected", Duration.ofSeconds(5));
        assertThat(hub.totalSubscribers()).isEqualTo(before + 1);

        bodies.forEach(java.util.stream.Stream::close);
        bodies.clear();

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (hub.totalSubscribers() > before && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }

        assertThat(hub.totalSubscribers())
                .as("the closed stream should have been reaped")
                .isEqualTo(before);
    }

    /** Issuing a ticket is an ordinary authenticated call and must stay one. */
    @Test
    void aTicketCannotBeObtainedWithoutSigningIn() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/realtime/ticket")))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    // -------------------------------------------------------------- helpers

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * Opens a real event-stream connection and drains it on a background thread.
     * {@code send} returns once the response headers arrive, which for a stream
     * is long before the body ends.
     */
    private BlockingQueue<JsonNode> openStream(String ticket) throws Exception {
        BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();

        HttpResponse<java.util.stream.Stream<String>> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/realtime/stream?ticket=" + ticket)))
                        .header("Accept", "text/event-stream")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofLines());

        assertThat(response.statusCode()).isEqualTo(200);

        java.util.stream.Stream<String> body = response.body();
        bodies.add(body);

        Thread reader = new Thread(() -> {
            try {
                body.forEach(line -> {
                    // Keep-alives arrive as ":" comments and carry nothing.
                    if (!line.startsWith("data:")) return;
                    try {
                        received.add(objectMapper.readTree(line.substring("data:".length())));
                    } catch (Exception ignored) {
                        // A partial line is not worth failing the test over.
                    }
                });
            } catch (Exception ignored) {
                // The connection is cut in tearDown; that is the expected end.
            }
        });
        reader.setDaemon(true);
        reader.start();
        readers.add(reader);

        return received;
    }

    /** Waits for one event of a type, discarding others, or null on timeout. */
    private JsonNode awaitEvent(BlockingQueue<JsonNode> stream, String type, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode event = stream.poll(100, TimeUnit.MILLISECONDS);
            if (event != null && type.equals(event.path("type").asText())) {
                return event;
            }
        }
        return null;
    }

    /** Everything that arrives within a window. Used to assert nothing does. */
    private List<JsonNode> drain(BlockingQueue<JsonNode> stream, Duration window)
            throws InterruptedException {
        List<JsonNode> collected = new ArrayList<>();
        long deadline = System.nanoTime() + window.toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode event = stream.poll(50, TimeUnit.MILLISECONDS);
            if (event != null) {
                collected.add(event);
            }
        }
        return collected;
    }

    private String ticketFor(String token) throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url("/api/realtime/ticket")))
                        .header("Authorization", "Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).get("data").get("ticket").asText();
    }

    private HttpResponse<String> postJson(String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String register(String farm) throws Exception {
        HttpResponse<String> response = postJson("/api/auth/register", null, """
                {"name":"Farmer %s","email":"%s-%d@example.com","password":"pass1234",
                 "role":"farmer","farmName":"%s"}"""
                .formatted(farm, farm, System.nanoTime(), farm));

        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).get("token").asText();
    }

    private Long createCow(String token, String prefix) throws Exception {
        HttpResponse<String> response = postJson("/api/cows", token, """
                {"tagId":"%s-%d","name":"Bessie","breed":"Nguni",
                 "dateOfBirth":"2021-01-01"}""".formatted(prefix, System.nanoTime()));

        assertThat(response.statusCode()).isEqualTo(201);
        return objectMapper.readTree(response.body()).get("data").get("cowId").asLong();
    }

    private void createGeofence(String token, Long cowId, String lat, String lng, int radius)
            throws Exception {
        HttpResponse<String> response = postJson("/api/geofences", token, """
                {"name":"Home camp","cowIds":[%d],"centerLatitude":%s,"centerLongitude":%s,
                 "radiusMeters":%d}""".formatted(cowId, lat, lng, radius));

        assertThat(response.statusCode()).isEqualTo(201);
    }

    private void recordPosition(String token, Long cowId, String lat, String lng) throws Exception {
        HttpResponse<String> response = postJson("/api/locations/record", token, """
                {"cowId":%d,"latitude":%s,"longitude":%s,"accuracy":10.0}"""
                .formatted(cowId, lat, lng));

        assertThat(response.statusCode()).isEqualTo(201);
    }
}
