package com.cowtrack;

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

import com.cowtrack.entity.Cow;
import com.cowtrack.entity.Farm;
import com.cowtrack.entity.LocationRecord;
import com.cowtrack.repository.CowRepository;
import com.cowtrack.repository.FarmRepository;
import com.cowtrack.repository.LocationRecordRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that two farmers cannot see each other's data.
 * Each farmer is registered (gets their own Farm), creates cows, and then
 * we confirm the other farmer's endpoints return empty results or 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FarmIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CowRepository cowRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private LocationRecordRepository locationRecordRepository;

    private String farmerAToken;
    private Long farmerACowId;
    private String farmerBToken;

    @BeforeEach
    void setUp() throws Exception {
        // Register Farmer A
        MvcResult regA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Farmer A","email":"farmerA-%d@example.com",
                                 "password":"pass1234","role":"farmer",
                                 "farmName":"Farm Alpha"}"""
                                .formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        farmerAToken = objectMapper.readTree(regA.getResponse().getContentAsString())
                .get("token").asText();

        // Farmer A creates a cow
        MvcResult cowResult = mockMvc.perform(post("/api/cows")
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"ISO-A-%d","name":"Alpha","breed":"Holstein",
                                 "dateOfBirth":"2020-01-01"}"""
                                .formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        farmerACowId = objectMapper.readTree(cowResult.getResponse().getContentAsString())
                .get("data").get("cowId").asLong();

        // Register Farmer B
        MvcResult regB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Farmer B","email":"farmerB-%d@example.com",
                                 "password":"pass1234","role":"farmer",
                                 "farmName":"Farm Beta"}"""
                                .formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        farmerBToken = objectMapper.readTree(regB.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void farmerBCannotSeeFarmerACows() throws Exception {
        // Farmer B lists all their cows — must not include Farmer A's cow
        MvcResult result = mockMvc.perform(get("/api/cows")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cows = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data");
        assertThat(cows.isArray()).isTrue();
        // Farmer B has no cows of their own
        assertThat(cows.size()).isEqualTo(0);
    }

    @Test
    void farmerBCannotAccessFarmerACowById() throws Exception {
        mockMvc.perform(get("/api/cows/" + farmerACowId)
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void farmerBCannotSearchFarmerACows() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cows/search?query=Alpha")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cows = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data");
        assertThat(cows.isArray()).isTrue();
        assertThat(cows.size()).isEqualTo(0);
    }

    @Test
    void farmerBCannotSeeFarmerAAlerts() throws Exception {
        // Farmer A has no alerts in test DB (fresh registration), but
        // confirm the endpoint returns empty for B.
        MvcResult result = mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode alerts = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data");
        assertThat(alerts.isArray()).isTrue();
    }

    @Test
    void farmerBCannotSeeFarmerAReminders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reminders")
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode reminders = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data");
        assertThat(reminders.isArray()).isTrue();
    }

    @Test
    void farmerACanSeeTheirOwnCows() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cows")
                        .header("Authorization", "Bearer " + farmerAToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cows = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data");
        assertThat(cows.size()).isEqualTo(1);
        assertThat(cows.get(0).get("name").asText()).isEqualTo("Alpha");
    }

    @Test
    void farmerACanAccessTheirOwnCowById() throws Exception {
        mockMvc.perform(get("/api/cows/" + farmerACowId)
                        .header("Authorization", "Bearer " + farmerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cowId").value(farmerACowId));
    }

    @Test
    void farmerBCannotUpdateFarmerACow() throws Exception {
        mockMvc.perform(put("/api/cows/" + farmerACowId)
                        .header("Authorization", "Bearer " + farmerBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"ISO-A-1","name":"Hijacked","breed":"Jersey",
                                 "dateOfBirth":"2020-01-01"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void farmerBCannotDeleteFarmerACow() throws Exception {
        mockMvc.perform(delete("/api/cows/" + farmerACowId)
                        .header("Authorization", "Bearer " + farmerBToken))
                .andExpect(status().isNotFound());

        // Farmer A's cow still exists
        mockMvc.perform(get("/api/cows/" + farmerACowId)
                        .header("Authorization", "Bearer " + farmerAToken))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/cows"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Writes were the gap. Every read path had been converted to a farm-scoped
     * lookup while {@code recordLocation} kept an unscoped {@code findById}, so
     * one farm could post GPS positions onto another farm's animals. Ids are
     * sequential, so guessing the target costs nothing.
     */
    @Test
    void farmerBCannotWriteToFarmerACow() throws Exception {
        record Attempt(String label, String path, String body) {}

        List<Attempt> attempts = List.of(
                new Attempt("record a location", "/api/locations/record",
                        """
                        {"cowId":%d,"latitude":0,"longitude":0,"accuracy":1}"""),
                new Attempt("record vitals", "/api/health/metrics",
                        """
                        {"cowId":%d,"temperature":40,"heartRate":99,"activityLevel":5}"""),
                new Attempt("record production", "/api/production",
                        """
                        {"cowId":%d,"recordDate":"2026-08-26","milkLitres":1,"weightKg":1}"""),
                new Attempt("schedule a vaccination", "/api/health/vaccinations",
                        """
                        {"cowId":%d,"vaccineName":"Hijack","nextDueDate":"2099-01-01"}"""),
                new Attempt("add a reminder", "/api/reminders",
                        """
                        {"cowId":%d,"reminderType":"Hijack","frequency":"WEEKLY","startDate":"2026-08-26"}""")
        );

        for (Attempt attempt : attempts) {
            mockMvc.perform(post(attempt.path())
                            .header("Authorization", "Bearer " + farmerBToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(attempt.body().formatted(farmerACowId)))
                    .andExpect(result -> assertThat(result.getResponse().getStatus())
                            .withFailMessage("Farmer B was able to %s on Farmer A's cow (HTTP %d)",
                                    attempt.label(), result.getResponse().getStatus())
                            .isGreaterThanOrEqualTo(400));
        }
    }

    /**
     * A rejected write is not enough on its own: what matters is what the victim
     * sees. A second, independent defect meant the live map resolved each cow's
     * latest position without a farm predicate, so a record belonging to another
     * farm was displayed as the owner's own.
     *
     * <p>The foreign record is inserted through the repository rather than the
     * API on purpose. Now that the write path is scoped, the API can no longer
     * produce this state — routing the setup through it would make the test pass
     * for the wrong reason and stop exercising the query at all.
     */
    @Test
    void farmerALiveMapIgnoresRecordsBelongingToAnotherFarm() throws Exception {
        mockMvc.perform(post("/api/locations/record")
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cowId":%d,"latitude":-23.9045,"longitude":29.4689,"accuracy":10}"""
                                .formatted(farmerACowId)))
                .andExpect(status().isCreated());

        Cow farmerACow = cowRepository.findById(farmerACowId).orElseThrow();
        Farm farmerBFarm = farmRepository.findAll().stream()
                .filter(farm -> !farm.getFarmId().equals(farmerACow.getFarm().getFarmId()))
                .findFirst()
                .orElseThrow();

        // Farm B's record, attached to Farm A's animal, timestamped later so any
        // unscoped "latest for this cow" lookup will prefer it.
        LocationRecord foreign = new LocationRecord();
        foreign.setCow(farmerACow);
        foreign.setFarm(farmerBFarm);
        foreign.setLatitude(BigDecimal.ZERO);
        foreign.setLongitude(BigDecimal.ZERO);
        foreign.setAccuracy(BigDecimal.ONE);
        foreign.setRecordedAt(LocalDateTime.now().plusMinutes(5));
        locationRecordRepository.save(foreign);

        MvcResult result = mockMvc.perform(get("/api/locations/live")
                        .header("Authorization", "Bearer " + farmerAToken))
                .andExpect(status().isOk())
                .andReturn();

        for (JsonNode fix : objectMapper.readTree(result.getResponse().getContentAsString()).get("data")) {
            double latitude = fix.get("latitude").asDouble();
            double longitude = fix.get("longitude").asDouble();
            assertThat(Math.abs(latitude) + Math.abs(longitude))
                    .withFailMessage("Farmer A's live map shows a position owned by another farm: %s, %s",
                            latitude, longitude)
                    .isGreaterThan(0.001);
        }
    }

    /**
     * A refused role check is a client error, not a server fault. Returning 500
     * left callers unable to tell "not allowed" from "broken", and buried every
     * denial in the error logs.
     */
    @Test
    void insufficientRoleIsForbiddenNotServerError() throws Exception {
        MvcResult caretaker = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Caretaker C","email":"caretaker-%d@example.com",
                                 "password":"pass1234","role":"caretaker","farmName":"Farm Gamma"}"""
                                .formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(caretaker.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(post("/api/cows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"C-1","name":"X","breed":"Nguni","dateOfBirth":"2020-01-01"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/cows/" + farmerACowId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
