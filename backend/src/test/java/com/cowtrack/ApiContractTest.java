package com.cowtrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Every path declared in {@code frontend/src/services/api.js} must be answered by
 * the backend. This test walks that list so a client call cannot silently 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long cowId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "contract-" + UUID.randomUUID() + "@example.com";

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Contract Tester","email":"%s","password":"secret123",
                                 "role":"farmer","phone":"0800123","farmName":"Contract Farm"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                // Profile fields the registration form sends must now be persisted.
                .andExpect(jsonPath("$.user.phone").value("0800123"))
                .andExpect(jsonPath("$.user.farmName").value("Contract Farm"))
                .andReturn();

        token = objectMapper.readTree(registered.getResponse().getContentAsString())
                .get("token").asText();

        MvcResult cow = mockMvc.perform(post("/api/cows")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"CT-%s","name":"Bessie","breed":"Holstein",
                                 "dateOfBirth":"2021-04-01"}"""
                                .formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated())
                // breed was previously accepted and silently dropped.
                .andExpect(jsonPath("$.data.breed").value("Holstein"))
                .andExpect(jsonPath("$.data.status").exists())
                .andReturn();

        cowId = objectMapper.readTree(cow.getResponse().getContentAsString())
                .get("data").get("cowId").asLong();
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private void expectOk(String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void analyticsEndpointsAllRespond() throws Exception {
        expectOk("/api/analytics/dashboard");
        expectOk("/api/analytics/production");
        expectOk("/api/analytics/health");
        expectOk("/api/analytics/financials");
        expectOk("/api/analytics/predictions");
    }

    @Test
    void healthEndpointsAllRespond() throws Exception {
        expectOk("/api/health/metrics");
        expectOk("/api/health/cows/" + cowId);
        expectOk("/api/health/vaccinations");
        expectOk("/api/health/reports");
    }

    @Test
    void settingsEndpointsAllRespond() throws Exception {
        expectOk("/api/settings/profile");
        expectOk("/api/settings/preferences");
        expectOk("/api/settings/export");
    }

    @Test
    void alertAndLocationEndpointsAllRespond() throws Exception {
        expectOk("/api/alerts");
        expectOk("/api/alerts/count/active");
        expectOk("/api/alerts/stats");
        expectOk("/api/locations/live");
        expectOk("/api/reminders");
        expectOk("/api/reminders/due");
    }

    /**
     * Framework-level request faults must report the caller's mistake, not a
     * server fault. Each of these previously surfaced as a 500.
     */
    @Test
    void malformedRequestsReportClientErrorsNotServerErrors() throws Exception {
        // Wrong verb on a real path.
        mockMvc.perform(post("/api/cows/" + cowId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        // Required query parameter omitted.
        mockMvc.perform(post("/api/alerts/test/geofence-breach/" + cowId)
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());

        // Body that is not valid JSON.
        mockMvc.perform(post("/api/cows").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());

        // Path variable of the wrong type.
        mockMvc.perform(get("/api/cows/not-a-number").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordingVitalsMakesThemVisibleOnTheCow() throws Exception {
        mockMvc.perform(post("/api/health/metrics")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cowId":%d,"temperature":39.1,"heartRate":72,"activityLevel":45}"""
                                .formatted(cowId)))
                .andExpect(status().isCreated())
                // All three vitals are out of band, so this must read as a warning.
                .andExpect(jsonPath("$.data.status").value("Warning"));

        mockMvc.perform(get("/api/cows/" + cowId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperature").value(39.1))
                .andExpect(jsonPath("$.data.age").exists())
                // Status must agree with the vitals, not contradict them.
                .andExpect(jsonPath("$.data.status").value("alert"));
    }

    @Test
    void schedulingAVaccinationDerivesItsStatus() throws Exception {
        mockMvc.perform(post("/api/health/vaccinations")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cowId":%d,"vaccineName":"Brucellosis",
                                 "nextDueDate":"2099-01-01"}""".formatted(cowId)))
                .andExpect(status().isCreated())
                // No administered date and a future due date means SCHEDULED.
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    void preferencesRoundTripAndDefaultBeforeAnySave() throws Exception {
        mockMvc.perform(get("/api/settings/preferences").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("light"));

        mockMvc.perform(put("/api/settings/preferences")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"theme":"dark","weeklyReports":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("dark"))
                .andExpect(jsonPath("$.data.weeklyReports").value(true))
                // Fields not sent keep their previous value.
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    void profileUpdateAcceptsTheClientsFieldNames() throws Exception {
        mockMvc.perform(put("/api/settings/profile")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed Farmer","location":"Limpopo"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Renamed Farmer"))
                .andExpect(jsonPath("$.data.location").value("Limpopo"));
    }

    @Test
    void passwordChangeRejectsAWrongCurrentPassword() throws Exception {
        mockMvc.perform(put("/api/settings/password")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"not-the-password","newPassword":"newsecret1"}"""))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/settings/password")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oldPassword":"secret123","newPassword":"newsecret1"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void bulkUpdateAppliesOnlyTheFieldsSupplied() throws Exception {
        mockMvc.perform(put("/api/cows/bulk")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"cowId":%d,"breed":"Jersey"}]""".formatted(cowId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].breed").value("Jersey"))
                // The name was not in the payload, so it must be untouched.
                .andExpect(jsonPath("$.data[0].name").value("Bessie"));
    }

    @Test
    void csvImportReportsPerRowOutcomes() throws Exception {
        String csv = """
                tagId,name,breed
                CSV-001,Clover,Angus
                ,Missing Tag,Angus
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "cows.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/upload/csv").file(file)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(1))
                // The bad row is reported rather than failing the whole upload.
                .andExpect(jsonPath("$.data.failed").value(1));
    }

    @Test
    void imageUploadRejectsANonImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notanimage.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/upload/image").file(file)
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }
}
