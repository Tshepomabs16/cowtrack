package com.cowtrack;

import com.cowtrack.config.NightMovementProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cowtrack.entity.*;
import com.cowtrack.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Night-movement detection.
 *
 * <p>Previously this computed the distance an animal had covered and then only
 * wrote it to the log, so {@code NIGHT_MOVEMENT} was an alert type nothing ever
 * produced.
 *
 * <p>The check is time-of-day dependent, which would otherwise make these tests
 * pass or fail according to when the suite happens to run. The night window is
 * therefore widened to cover the whole day for the duration of each test, and
 * narrowed to nothing where the test needs daytime — the boundary logic itself
 * is exercised directly rather than by waiting for 2am.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NightMovementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NightMovementProperties properties;
    @Autowired private FarmRepository farmRepository;
    @Autowired private CowRepository cowRepository;
    @Autowired private LocationRecordRepository locationRepository;
    @Autowired private AlertRepository alertRepository;

    private String token;
    private Farm farm;
    private Cow cow;
    private int originalStart;
    private int originalEnd;

    @BeforeEach
    void setUp() throws Exception {
        originalStart = properties.getStartHour();
        originalEnd = properties.getEndHour();
        // Always night: the non-wrapping branch reads hour >= 0 && hour < 24.
        // Setting both to 0 instead yields hour >= 0 && hour < 0, an empty
        // window, which is how the daytime case below is expressed.
        properties.setStartHour(0);
        properties.setEndHour(24);

        // Registered through the API rather than built by hand: FarmContext reads
        // the farm from the JWT in the authentication credentials, so a
        // hand-assembled SecurityContext without a real token is rejected.
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Night Farmer","email":"night-%d@example.com",
                                 "password":"pass1234","role":"farmer","farmName":"Night Farm %d"}"""
                                .formatted(System.nanoTime(), System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(registered.getResponse().getContentAsString())
                .get("token").asText();

        MvcResult created = mockMvc.perform(post("/api/cows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"NIGHT-%d","name":"Wanderer","breed":"Nguni",
                                 "dateOfBirth":"2021-01-01"}""".formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        Long cowId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("cowId").asLong();

        cow = cowRepository.findById(cowId).orElseThrow();
        farm = cow.getFarm();
    }

    @AfterEach
    void tearDown() {
        properties.setStartHour(originalStart);
        properties.setEndHour(originalEnd);
    }

    /** A prior position, backdated so the window covers it. */
    private void existingPositionAt(double lat, double lng, int minutesAgo) {
        LocationRecord record = new LocationRecord();
        record.setCow(cow);
        record.setFarm(farm);
        record.setLatitude(BigDecimal.valueOf(lat));
        record.setLongitude(BigDecimal.valueOf(lng));
        record.setAccuracy(BigDecimal.TEN);
        record.setRecordedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        locationRepository.save(record);
    }

    /** A live position through the API, which is what triggers detection. */
    private void reportPosition(double lat, double lng) throws Exception {
        mockMvc.perform(post("/api/locations/record")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cowId":%d,"latitude":%s,"longitude":%s,"accuracy":10}"""
                                .formatted(cow.getCowId(), lat, lng)))
                .andExpect(status().isCreated());
    }

    private List<Alert> nightAlerts() {
        return alertRepository.findAll().stream()
                .filter(alert -> alert.getAlertType() == Alert.AlertType.NIGHT_MOVEMENT)
                .filter(alert -> alert.getCow().getCowId().equals(cow.getCowId()))
                .toList();
    }

    /**
     * The case that previously only reached the log. Roughly 900 m of travel,
     * comfortably over the 100 m threshold.
     */
    @Test
    void raisesAnAlertWhenAnAnimalIsMovedAtNight() throws Exception {
        existingPositionAt(-23.9045, 29.4689, 30);
        reportPosition(-23.9125, 29.4689);

        assertThat(nightAlerts())
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.getIsResolved()).isFalse();
                    assertThat(alert.getFarm().getFarmId()).isEqualTo(farm.getFarmId());
                    assertThat(alert.getMessage()).contains("during night hours");
                });
    }

    /** The distance is what tells a farmer whether it is worth getting up for. */
    @Test
    void reportsHowFarTheAnimalMoved() throws Exception {
        existingPositionAt(-23.9045, 29.4689, 30);
        reportPosition(-23.9125, 29.4689);

        assertThat(nightAlerts().get(0).getMessage())
                .matches(".*moved (\\d+ m|[\\d.]+ km) during night hours.*");
    }

    /** Cattle shuffling at a trough must not wake anybody. */
    @Test
    void ignoresSmallMovementsWithinTheThreshold() throws Exception {
        existingPositionAt(-23.9045, 29.4689, 30);
        reportPosition(-23.90455, 29.46895);

        assertThat(nightAlerts()).isEmpty();
    }

    /**
     * The check runs on every incoming position, so a collar reporting every few
     * minutes would otherwise raise an alert per position for one event.
     */
    @Test
    void raisesOnlyOneAlertPerNightHoweverOftenTheCollarReports() throws Exception {
        existingPositionAt(-23.9045, 29.4689, 30);

        reportPosition(-23.9125, 29.4689);
        reportPosition(-23.9200, 29.4689);
        reportPosition(-23.9275, 29.4689);

        assertThat(nightAlerts()).hasSize(1);
    }

    /** Daytime movement is grazing, not theft. */
    @Test
    void ignoresTheSameMovementDuringTheDay() throws Exception {
        // Empty window: start == end in the non-wrapping form is never night.
        properties.setStartHour(3);
        properties.setEndHour(3);

        existingPositionAt(-23.9045, 29.4689, 30);
        reportPosition(-23.9125, 29.4689);

        assertThat(nightAlerts()).isEmpty();
    }

    @Test
    void doesNothingWhenTheCheckIsDisabled() throws Exception {
        properties.setEnabled(false);
        try {
            existingPositionAt(-23.9045, 29.4689, 30);
            reportPosition(-23.9125, 29.4689);
            assertThat(nightAlerts()).isEmpty();
        } finally {
            properties.setEnabled(true);
        }
    }

    /**
     * A single position cannot describe movement, and the very first report from
     * a newly fitted collar must not look like an animal being driven.
     */
    @Test
    void needsAtLeastTwoPositionsBeforeItCanJudge() throws Exception {
        reportPosition(-23.9045, 29.4689);

        assertThat(nightAlerts()).isEmpty();
    }

    /**
     * Severity drives how urgently the alert is treated, so the mapping is pinned
     * rather than left to be quietly downgraded. Night movement ranks with a
     * boundary breach: both mean the animal is being taken now.
     */
    @Test
    void nightMovementIsCritical() throws Exception {
        existingPositionAt(-23.9045, 29.4689, 30);
        reportPosition(-23.9125, 29.4689);

        MvcResult result = mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode alerts = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        JsonNode nightAlert = null;
        for (JsonNode alert : alerts) {
            if ("NIGHT_MOVEMENT".equals(alert.get("alertType").asText())) {
                nightAlert = alert;
            }
        }

        assertThat(nightAlert)
                .withFailMessage("No night-movement alert was returned by the API")
                .isNotNull();
        assertThat(nightAlert.get("severity").asText()).isEqualTo("critical");
    }
}
