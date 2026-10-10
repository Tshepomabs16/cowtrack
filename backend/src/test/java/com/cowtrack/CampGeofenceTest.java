package com.cowtrack;

import com.cowtrack.entity.Alert;
import com.cowtrack.repository.AlertRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Camps and restricted zones, end to end: drawn through the API, fed by a
 * collar through ingestion, and judged by what lands in the alert feed.
 *
 * <p>The camp is a square about 1.1 km across centred on the farm. Positions
 * are given as metres from its northern edge so each test reads as what the
 * animal did rather than as coordinates.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampGeofenceTest {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final double NORTH_EDGE = -23.8995;
    private static final double LON = 29.4689;
    private static final double METRE = 1.0 / 111_195.0;

    private static final String SQUARE_CAMP = """
            [[-23.8995,29.4634],[-23.8995,29.4744],[-23.9095,29.4744],[-23.9095,29.4634]]""";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AlertRepository alertRepository;

    private String token;
    private Long cowId;
    private String serial;
    private String apiKey;
    private LocalDateTime clock;

    @BeforeEach
    void setUp() throws Exception {
        token = register("farmer", "Camp Farm");
        cowId = createCow(token);

        serial = "CAMP-" + System.nanoTime();
        MvcResult device = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNumber":"%s","cowId":%d}""".formatted(serial, cowId)))
                .andExpect(status().isCreated())
                .andReturn();
        apiKey = data(device).get("apiKey").asText();

        // Readings are spaced a minute apart, ending a little before now.
        clock = LocalDateTime.now().minusHours(2);
    }

    // --- drawing camps ---------------------------------------------------------

    @Test
    void aPolygonCampIsStoredWithItsCornersAndAnimals() throws Exception {
        JsonNode camp = createCamp(SQUARE_CAMP, cowId);

        assertThat(camp.get("name").asText()).isEqualTo("Home camp");
        assertThat(camp.get("fenceType").asText()).isEqualTo("KEEP_IN");
        assertThat(camp.get("shape").asText()).isEqualTo("POLYGON");
        assertThat(camp.get("vertices")).hasSize(4);
        assertThat(camp.get("isActive").asBoolean()).isTrue();
        assertThat(camp.get("animalCount").asLong()).isEqualTo(1);

        mockMvc.perform(get("/api/cows/" + cowId).header("Authorization", bearer()))
                .andExpect(jsonPath("$.data.campId").value(camp.get("geofenceId").asLong()))
                .andExpect(jsonPath("$.data.campName").value("Home camp"));

        mockMvc.perform(get("/api/geofences").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void aPolygonWhoseEdgesCrossIsRefused() throws Exception {
        mockMvc.perform(post("/api/geofences").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bow tie","shape":"POLYGON",
                                 "vertices":[[-23.90,29.46],[-23.91,29.47],[-23.90,29.47],[-23.91,29.46]]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cross")));
    }

    @Test
    void shapesThatCannotBeEvaluatedAreRefused() throws Exception {
        String[] invalid = {
                // circle without a radius
                """
                {"name":"No radius","shape":"CIRCLE","centerLatitude":-23.9,"centerLongitude":29.4}""",
                // a centre at 0,0 is a missing position, not a camp in the Atlantic
                """
                {"name":"Null island","shape":"CIRCLE","centerLatitude":0,"centerLongitude":0,"radiusMeters":100}""",
                // two corners are a line
                """
                {"name":"Line","shape":"POLYGON","vertices":[[-23.90,29.46],[-23.91,29.47]]}""",
                // unknown type
                """
                {"name":"Odd","fenceType":"SOMETIMES","centerLatitude":-23.9,"centerLongitude":29.4,"radiusMeters":100}""",
                // no name
                """
                {"shape":"CIRCLE","centerLatitude":-23.9,"centerLongitude":29.4,"radiusMeters":100}"""
        };
        for (String body : invalid) {
            mockMvc.perform(post("/api/geofences").header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void aCaretakerCanSeeCampsButNotDrawThem() throws Exception {
        String caretaker = register("caretaker", "Caretaker Farm");

        mockMvc.perform(get("/api/geofences").header("Authorization", "Bearer " + caretaker))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/geofences").header("Authorization", "Bearer " + caretaker)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mine","centerLatitude":-23.9,"centerLongitude":29.4,"radiusMeters":100}"""))
                .andExpect(status().isForbidden());
    }

    // --- leaving and returning -------------------------------------------------

    @Test
    void leavingTheCampRaisesOneAlertAndComingBackClosesIt() throws Exception {
        long campId = createCamp(SQUARE_CAMP, cowId).get("geofenceId").asLong();

        report(-300);          // grazing inside
        report(400);           // through the fence and away
        report(600);           // still out: must not raise a second alert

        List<Alert> open = openBreaches();
        assertThat(open).hasSize(1);
        assertThat(open.get(0).getGeofence().getGeofenceId()).isEqualTo(campId);
        assertThat(open.get(0).getMessage()).contains("left camp 'Home camp'").contains("outside it");

        report(-250);          // back home

        assertThat(openBreaches()).isEmpty();
        Alert closed = breaches().get(0);
        assertThat(closed.getIsResolved()).isTrue();
        assertThat(closed.getResolvedAt()).isNotNull();
        assertThat(closed.getResolutionNote()).startsWith("Returned inside camp 'Home camp'");
    }

    @Test
    void returningToTheCampIsNotItselfABreach() throws Exception {
        createCamp(SQUARE_CAMP, cowId);

        report(-300);
        report(-200);
        report(-100);

        assertThat(breaches()).isEmpty();
    }

    @Test
    void aCollarWanderingAcrossTheFenceLineDoesNotAlert() throws Exception {
        createCamp(SQUARE_CAMP, cowId);

        // A cow grazing along the fence, its fixes landing either side of it.
        report(-20);
        report(8);
        report(-5);
        report(12);
        report(-10);

        assertThat(breaches()).isEmpty();
    }

    @Test
    void twoFixesInARowJustOutsideDoConfirmTheBreach() throws Exception {
        createCamp(SQUARE_CAMP, cowId);

        report(-50);
        report(12);
        assertThat(breaches()).isEmpty();
        report(10);

        assertThat(openBreaches()).hasSize(1);
    }

    @Test
    void aLateUploadOfOldPositionsDoesNotReopenAnOldCrossing() throws Exception {
        createCamp(SQUARE_CAMP, cowId);
        LocalDateTime start = clock;
        report(-300);
        report(-200);

        // The collar now uploads a fix from before both of those, from outside.
        upload(start.minusMinutes(30), NORTH_EDGE + 500 * METRE);

        assertThat(breaches()).isEmpty();
    }

    @Test
    void enteringARestrictedZoneAlertsAndLeavingItClears() throws Exception {
        createCamp(SQUARE_CAMP, cowId);
        // A 60 m dam 300 m inside the camp's northern edge.
        mockMvc.perform(post("/api/geofences").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dam","fenceType":"KEEP_OUT","shape":"CIRCLE",
                                 "centerLatitude":%s,"centerLongitude":%s,"radiusMeters":60}"""
                                .formatted(NORTH_EDGE - 300 * METRE, LON)))
                .andExpect(status().isCreated());

        report(-100);          // in the camp, clear of the dam
        report(-300);          // in the middle of the dam

        List<Alert> open = openBreaches();
        assertThat(open).hasSize(1);
        assertThat(open.get(0).getMessage()).contains("entered restricted area 'Dam'");

        report(-500);          // out of the dam, still in the camp
        assertThat(openBreaches()).isEmpty();
        assertThat(breaches().get(0).getResolutionNote()).startsWith("Left restricted area 'Dam'");
    }

    // --- rotation and removal --------------------------------------------------

    @Test
    void aCampCannotBeSwitchedOffWithAnimalsStillInIt() throws Exception {
        long campId = createCamp(SQUARE_CAMP, cowId).get("geofenceId").asLong();

        mockMvc.perform(post("/api/geofences/" + campId + "/deactivate").header("Authorization", bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1 animal")));

        mockMvc.perform(delete("/api/geofences/" + campId + "/animals/" + cowId).header("Authorization", bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/geofences/" + campId + "/deactivate").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
        mockMvc.perform(get("/api/geofences/" + campId).header("Authorization", bearer()))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void aSwitchedOffRestrictedZoneIsNotEvaluated() throws Exception {
        MvcResult dam = mockMvc.perform(post("/api/geofences").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dam","fenceType":"KEEP_OUT","centerLatitude":%s,
                                 "centerLongitude":%s,"radiusMeters":60}""".formatted(NORTH_EDGE, LON)))
                .andExpect(status().isCreated())
                .andReturn();
        long damId = data(dam).get("geofenceId").asLong();

        mockMvc.perform(post("/api/geofences/" + damId + "/deactivate").header("Authorization", bearer()))
                .andExpect(status().isOk());

        report(0);             // standing in the middle of it

        assertThat(breaches()).isEmpty();
    }

    @Test
    void movingAnAnimalToAnotherCampClosesItsBreachOfTheOldOne() throws Exception {
        createCamp(SQUARE_CAMP, cowId);
        report(-300);
        report(800);
        assertThat(openBreaches()).hasSize(1);

        // The farmer has driven it to the north camp, which is where it now is.
        long north = createCamp("North camp", """
                [[-23.8900,29.4634],[-23.8900,29.4744],[-23.8994,29.4744],[-23.8994,29.4634]]""", null)
                .get("geofenceId").asLong();
        mockMvc.perform(post("/api/geofences/" + north + "/animals").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cowIds\":[" + cowId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animalCount").value(1));

        assertThat(openBreaches()).isEmpty();
        assertThat(breaches().get(0).getResolutionNote()).isEqualTo("Moved to camp 'North camp'");

        report(900);           // inside the north camp: no alert from either
        assertThat(openBreaches()).isEmpty();
    }

    @Test
    void aCampThatHasRaisedAlertsIsRetiredRatherThanDeleted() throws Exception {
        long campId = createCamp(SQUARE_CAMP, cowId).get("geofenceId").asLong();
        report(-300);
        report(800);
        mockMvc.perform(delete("/api/geofences/" + campId + "/animals/" + cowId).header("Authorization", bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/geofences/" + campId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(false))
                .andExpect(jsonPath("$.data.retired").value(true));

        // Gone from the map, still there for the record.
        mockMvc.perform(get("/api/geofences").header("Authorization", bearer()))
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/geofences?includeRetired=true").header("Authorization", bearer()))
                .andExpect(jsonPath("$.data[0].retiredAt").exists());
        assertThat(breaches()).hasSize(1);
        assertThat(breaches().get(0).getGeofence().getGeofenceId()).isEqualTo(campId);
    }

    @Test
    void aCampWithNoHistoryIsDeletedOutright() throws Exception {
        long campId = createCamp(SQUARE_CAMP, null).get("geofenceId").asLong();

        mockMvc.perform(delete("/api/geofences/" + campId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
        mockMvc.perform(get("/api/geofences/" + campId).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    // --- isolation -------------------------------------------------------------

    @Test
    void anotherFarmCannotReachThisFarmsCampsOrAnimals() throws Exception {
        long campId = createCamp(SQUARE_CAMP, cowId).get("geofenceId").asLong();
        String other = "Bearer " + register("farmer", "Other Farm");
        long otherCow = createCow(other.substring("Bearer ".length()));

        mockMvc.perform(get("/api/geofences/" + campId).header("Authorization", other))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/geofences/" + campId).header("Authorization", other))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/geofences/" + campId + "/animals").header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cowIds\":[" + otherCow + "]}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/geofences").header("Authorization", other))
                .andExpect(jsonPath("$.data.length()").value(0));

        // Nor can this farm pull the other farm's animal into its own camp.
        mockMvc.perform(post("/api/geofences/" + campId + "/animals").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cowIds\":[" + otherCow + "]}"))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---------------------------------------------------------------

    private String bearer() {
        return "Bearer " + token;
    }

    private String register(String role, String farm) throws Exception {
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Camp Tester","email":"camp-%d@example.com","password":"pass1234",
                                 "role":"%s","farmName":"%s"}""".formatted(System.nanoTime(), role, farm)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(registered.getResponse().getContentAsString()).get("token").asText();
    }

    private long createCow(String withToken) throws Exception {
        MvcResult cow = mockMvc.perform(post("/api/cows")
                        .header("Authorization", "Bearer " + withToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"CAMP-%d","name":"Ntombi","breed":"Nguni",
                                 "dateOfBirth":"2021-01-01"}""".formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        return data(cow).get("cowId").asLong();
    }

    private JsonNode createCamp(String vertices, Long withCow) throws Exception {
        return createCamp("Home camp", vertices, withCow);
    }

    private JsonNode createCamp(String name, String vertices, Long withCow) throws Exception {
        String cows = withCow == null ? "" : ",\"cowIds\":[" + withCow + "]";
        MvcResult camp = mockMvc.perform(post("/api/geofences").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"shape\":\"POLYGON\",\"vertices\":" + vertices + cows + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return data(camp);
    }

    /**
     * The collar reports one fix, the given number of metres north of the camp's
     * northern edge: negative is inside the camp, positive is outside it.
     */
    private void report(double metresNorthOfEdge) throws Exception {
        clock = clock.plusMinutes(1);
        upload(clock, NORTH_EDGE + metresNorthOfEdge * METRE);
    }

    private void upload(LocalDateTime at, double latitude) throws Exception {
        mockMvc.perform(post("/api/ingest/readings")
                        .header("X-Device-Serial", serial)
                        .header("X-Device-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readings":[{"recordedAt":"%s","latitude":%s,"longitude":%s,"accuracy":10}]}"""
                                .formatted(at.format(ISO), BigDecimal.valueOf(latitude)
                                        .setScale(7, RoundingMode.HALF_UP).toPlainString(), LON)))
                .andExpect(status().isOk());
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private List<Alert> breaches() {
        return alertRepository.findByCowCowIdOrderByCreatedAtDesc(cowId).stream()
                .filter(alert -> alert.getAlertType() == Alert.AlertType.GEOFENCE_BREACH)
                .toList();
    }

    private List<Alert> openBreaches() {
        return breaches().stream().filter(alert -> !alert.getIsResolved()).toList();
    }
}
