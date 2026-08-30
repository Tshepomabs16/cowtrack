package com.cowtrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cowtrack.entity.Alert;
import com.cowtrack.repository.AlertRepository;
import com.cowtrack.repository.LocationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Device ingestion.
 *
 * <p>The behaviours worth protecting here are the tolerant ones. A collar cannot
 * be reasoned with: it buffers while out of coverage, resends anything it was
 * not acknowledged for, and keeps time badly. The endpoint has to absorb all of
 * that without either losing good readings or letting a bad one corrupt the
 * animal's history.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceIngestionTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LocationRecordRepository locationRepository;
    @Autowired private AlertRepository alertRepository;

    private String token;
    private Long cowId;
    private Long deviceId;
    private String serial;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Device Farmer","email":"device-%d@example.com",
                                 "password":"pass1234","role":"farmer","farmName":"Device Farm %d"}"""
                                .formatted(System.nanoTime(), System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(registered.getResponse().getContentAsString())
                .get("token").asText();

        MvcResult cow = mockMvc.perform(post("/api/cows")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"DEV-%d","name":"Tracked","breed":"Nguni",
                                 "dateOfBirth":"2021-01-01"}""".formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        cowId = objectMapper.readTree(cow.getResponse().getContentAsString())
                .get("data").get("cowId").asLong();

        serial = "COLLAR-" + System.nanoTime();
        MvcResult device = mockMvc.perform(post("/api/devices")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNumber":"%s","cowId":%d}""".formatted(serial, cowId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(device.getResponse().getContentAsString()).get("data");
        deviceId = body.get("deviceId").asLong();
        apiKey = body.get("apiKey").asText();
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private String reading(LocalDateTime at, double lat, double lng) {
        return """
                {"recordedAt":"%s","latitude":%s,"longitude":%s,"accuracy":10}"""
                .formatted(at.format(ISO), lat, lng);
    }

    private MvcResult upload(String body) throws Exception {
        return upload(serial, apiKey, body);
    }

    private MvcResult upload(String withSerial, String withKey, String body) throws Exception {
        return mockMvc.perform(post("/api/ingest/readings")
                        .header("X-Device-Serial", withSerial)
                        .header("X-Device-Key", withKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private JsonNode resultOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private long storedPositions() {
        return locationRepository.findAll().stream()
                .filter(record -> record.getCow().getCowId().equals(cowId))
                .count();
    }

    // --- the key is the credential -------------------------------------------

    @Test
    void acceptsABatchFromAnAuthenticatedCollar() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        MvcResult result = upload("""
                {"batteryPercent":84,"firmwareVersion":"1.4.2","readings":[%s,%s]}"""
                .formatted(reading(now.minusMinutes(20), -23.9045, 29.4689),
                           reading(now.minusMinutes(10), -23.9048, 29.4692)));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode data = resultOf(result);
        assertThat(data.get("accepted").asInt()).isEqualTo(2);
        assertThat(data.get("rejected").asInt()).isZero();
        assertThat(storedPositions()).isEqualTo(2);
    }

    @Test
    void refusesAWrongKey() throws Exception {
        MvcResult result = upload(serial, "not-the-key", """
                {"readings":[%s]}""".formatted(reading(LocalDateTime.now(), -23.9, 29.4)));

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(storedPositions()).isZero();
    }

    /** An unknown serial must not be distinguishable from a wrong key. */
    @Test
    void refusesAnUnknownSerialTheSameWayAsAWrongKey() throws Exception {
        MvcResult unknown = upload("COLLAR-does-not-exist", apiKey, """
                {"readings":[%s]}""".formatted(reading(LocalDateTime.now(), -23.9, 29.4)));
        MvcResult wrongKey = upload(serial, "not-the-key", """
                {"readings":[%s]}""".formatted(reading(LocalDateTime.now(), -23.9, 29.4)));

        assertThat(unknown.getResponse().getStatus())
                .isEqualTo(wrongKey.getResponse().getStatus())
                .isEqualTo(401);
    }

    @Test
    void refusesAKeyThatHasBeenRotated() throws Exception {
        mockMvc.perform(put("/api/devices/" + deviceId + "/rotate-key")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());

        MvcResult result = upload("""
                {"readings":[%s]}""".formatted(reading(LocalDateTime.now(), -23.9, 29.4)));

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void refusesARetiredCollar() throws Exception {
        mockMvc.perform(put("/api/devices/" + deviceId + "/retire")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());

        assertThat(upload("""
                {"readings":[%s]}""".formatted(reading(LocalDateTime.now(), -23.9, 29.4)))
                .getResponse().getStatus()).isEqualTo(401);
    }

    // --- tolerance ------------------------------------------------------------

    /**
     * A collar that does not get an acknowledgement resends. The same batch
     * arriving twice must not double the animal's history.
     */
    @Test
    void treatsAResentBatchAsDuplicatesRatherThanNewReadings() throws Exception {
        LocalDateTime at = LocalDateTime.now().minusMinutes(15);
        String batch = """
                {"readings":[%s]}""".formatted(reading(at, -23.9045, 29.4689));

        assertThat(resultOf(upload(batch)).get("accepted").asInt()).isEqualTo(1);

        JsonNode second = resultOf(upload(batch));
        assertThat(second.get("accepted").asInt()).isZero();
        assertThat(second.get("duplicates").asInt()).isEqualTo(1);
        assertThat(storedPositions()).isEqualTo(1);
    }

    /**
     * One malformed row must not cost the good rows in the same upload: the
     * device would clear its buffer either way, so rejecting the batch loses
     * them permanently.
     */
    @Test
    void keepsTheGoodReadingsWhenOneInTheBatchIsBad() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        MvcResult result = upload("""
                {"readings":[
                  %s,
                  {"recordedAt":"%s","latitude":999,"longitude":29.4689},
                  %s
                ]}"""
                .formatted(reading(now.minusMinutes(20), -23.9045, 29.4689),
                           now.minusMinutes(15).format(ISO),
                           reading(now.minusMinutes(10), -23.9048, 29.4692)));

        JsonNode data = resultOf(result);
        assertThat(data.get("accepted").asInt()).isEqualTo(2);
        assertThat(data.get("rejected").asInt()).isEqualTo(1);
        assertThat(data.get("errors").get(0).get("reason").asText()).contains("Latitude");
        assertThat(storedPositions()).isEqualTo(2);
    }

    /**
     * A collar with a broken clock could otherwise write a position dated next
     * year, which would win every "latest position" query forever — freezing the
     * live map and suppressing no-signal detection for that animal.
     */
    @Test
    void refusesAReadingDatedInTheFuture() throws Exception {
        MvcResult result = upload("""
                {"readings":[%s]}"""
                .formatted(reading(LocalDateTime.now().plusDays(400), -23.9045, 29.4689)));

        JsonNode data = resultOf(result);
        assertThat(data.get("rejected").asInt()).isEqualTo(1);
        assertThat(data.get("errors").get(0).get("reason").asText()).contains("future");
        assertThat(storedPositions()).isZero();
    }

    @Test
    void refusesAReadingCarryingNeitherPositionNorVitals() throws Exception {
        MvcResult result = upload("""
                {"readings":[{"recordedAt":"%s"}]}"""
                .formatted(LocalDateTime.now().format(ISO)));

        assertThat(resultOf(result).get("rejected").asInt()).isEqualTo(1);
    }

    /** Half a coordinate is a device bug; dropping it silently would hide it. */
    @Test
    void refusesAHalfSuppliedCoordinate() throws Exception {
        MvcResult result = upload("""
                {"readings":[{"recordedAt":"%s","latitude":-23.9045}]}"""
                .formatted(LocalDateTime.now().format(ISO)));

        JsonNode data = resultOf(result);
        assertThat(data.get("rejected").asInt()).isEqualTo(1);
        assertThat(data.get("errors").get(0).get("reason").asText()).contains("together");
    }

    /** Collars send vitals without a fix when they cannot see satellites. */
    @Test
    void acceptsVitalsWithoutAPosition() throws Exception {
        MvcResult result = upload("""
                {"readings":[{"recordedAt":"%s","temperature":38.4,"heartRate":66,"activityLevel":80}]}"""
                .formatted(LocalDateTime.now().minusMinutes(5).format(ISO)));

        assertThat(resultOf(result).get("accepted").asInt()).isEqualTo(1);
        assertThat(storedPositions()).isZero();

        mockMvc.perform(get("/api/health/cows/" + cowId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].temperature").value(38.4));
    }

    // --- ordering -------------------------------------------------------------

    /**
     * A backfill of older readings must not trigger alert evaluation: the
     * animal's current position has not changed, and re-evaluating against a
     * boundary it crossed days ago would raise a breach for history.
     */
    @Test
    void doesNotEvaluateAlertsWhenOnlyBackfillingOlderReadings() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        assertThat(resultOf(upload("""
                {"readings":[%s]}""".formatted(reading(now.minusMinutes(5), -23.9045, 29.4689))))
                .get("alertsEvaluated").asBoolean()).isTrue();

        JsonNode backfill = resultOf(upload("""
                {"readings":[%s]}""".formatted(reading(now.minusHours(6), -23.9100, 29.4700))));

        assertThat(backfill.get("accepted").asInt()).isEqualTo(1);
        assertThat(backfill.get("alertsEvaluated").asBoolean())
                .withFailMessage("Backfilling an older reading should not re-evaluate alerts")
                .isFalse();
    }

    /** The payload order is not trusted; readings are stored oldest first. */
    @Test
    void storesAnOutOfOrderBatchInChronologicalOrder() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        upload("""
                {"readings":[%s,%s,%s]}"""
                .formatted(reading(now.minusMinutes(5), -23.9048, 29.4692),
                           reading(now.minusMinutes(25), -23.9045, 29.4689),
                           reading(now.minusMinutes(15), -23.9046, 29.4690)));

        MvcResult history = mockMvc.perform(get("/api/locations/cow/" + cowId + "/history")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode records = objectMapper.readTree(history.getResponse().getContentAsString()).get("data");
        assertThat(records).hasSize(3);

        // LocationResponse formats timestamps as "yyyy-MM-dd HH:mm:ss" rather than
        // ISO-8601, so the response cannot be parsed with LocalDateTime.parse.
        DateTimeFormatter responseFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // The history endpoint returns newest first.
        LocalDateTime previous = null;
        for (JsonNode record : records) {
            LocalDateTime at = LocalDateTime.parse(record.get("recordedAt").asText(), responseFormat);
            if (previous != null) {
                assertThat(at).isBeforeOrEqualTo(previous);
            }
            previous = at;
        }
    }

    // --- device state ---------------------------------------------------------

    @Test
    void recordsBatteryFirmwareAndLastSeen() throws Exception {
        upload("""
                {"batteryPercent":73,"firmwareVersion":"2.0.1","readings":[%s]}"""
                .formatted(reading(LocalDateTime.now().minusMinutes(2), -23.9045, 29.4689)));

        MvcResult devices = mockMvc.perform(get("/api/devices").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode device = objectMapper.readTree(devices.getResponse().getContentAsString())
                .get("data").get(0);
        assertThat(device.get("batteryPercent").asInt()).isEqualTo(73);
        assertThat(device.get("firmwareVersion").asText()).isEqualTo("2.0.1");
        assertThat(device.get("lastSeenAt").isNull()).isFalse();
        assertThat(device.get("batteryLow").asBoolean()).isFalse();
    }

    /** A dying collar is the precursor to a silent one, so it is worth knowing. */
    @Test
    void raisesAnAlertWhenTheBatteryGetsLow() throws Exception {
        upload("""
                {"batteryPercent":12,"readings":[%s]}"""
                .formatted(reading(LocalDateTime.now().minusMinutes(2), -23.9045, 29.4689)));

        assertThat(lowBatteryAlerts()).hasSize(1);
    }

    /** A flat collar uploads all day; repeating the alert would bury the feed. */
    @Test
    void raisesTheLowBatteryAlertOnceRatherThanEveryBatch() throws Exception {
        for (int i = 5; i >= 1; i--) {
            upload("""
                    {"batteryPercent":11,"readings":[%s]}"""
                    .formatted(reading(LocalDateTime.now().minusMinutes(i), -23.9045, 29.4689)));
        }

        assertThat(lowBatteryAlerts()).hasSize(1);
    }

    private java.util.List<Alert> lowBatteryAlerts() {
        return alertRepository.findAll().stream()
                .filter(alert -> alert.getAlertType() == Alert.AlertType.LOW_BATTERY)
                .filter(alert -> alert.getCow().getCowId().equals(cowId))
                .toList();
    }

    /** A collar in a store has no animal to attribute readings to. */
    @Test
    void refusesReadingsFromACollarNotFittedToAnAnimal() throws Exception {
        mockMvc.perform(put("/api/devices/" + deviceId + "/unassign")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());

        MvcResult result = upload("""
                {"readings":[%s]}""".formatted(reading(LocalDateTime.now(), -23.9045, 29.4689)));

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(storedPositions()).isZero();
    }
}
