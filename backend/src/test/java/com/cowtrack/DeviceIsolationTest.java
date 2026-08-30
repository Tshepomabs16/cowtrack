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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Devices are a new way into the data, so they are a new way to get tenancy
 * wrong. A collar authenticates without a JWT and then acts as its farm, which
 * makes it worth proving that farm is the device's own and not one an attacker
 * chose.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceIsolationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String tokenA;
    private String tokenB;
    private Long cowA;
    private Long deviceA;
    private String serialA;
    private String keyA;

    @BeforeEach
    void setUp() throws Exception {
        tokenA = register("Farmer Alpha", "Alpha");
        tokenB = register("Farmer Beta", "Beta");

        cowA = createCow(tokenA, "ISO-DEV-A");

        serialA = "COLLAR-A-" + System.nanoTime();
        MvcResult device = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNumber":"%s","cowId":%d}""".formatted(serialA, cowA)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(device.getResponse().getContentAsString()).get("data");
        deviceA = body.get("deviceId").asLong();
        keyA = body.get("apiKey").asText();
    }

    private String register(String name, String farm) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s-%d@example.com","password":"pass1234",
                                 "role":"farmer","farmName":"%s"}"""
                                .formatted(name, farm, System.nanoTime(), farm)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createCow(String token, String prefix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"%s-%d","name":"Beast","breed":"Nguni",
                                 "dateOfBirth":"2021-01-01"}""".formatted(prefix, System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("cowId").asLong();
    }

    @Test
    void farmerBCannotSeeFarmerADevices() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/devices")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).get("data")).isEmpty();
    }

    @Test
    void farmerBCannotRotateOrRetireFarmerADevice() throws Exception {
        mockMvc.perform(put("/api/devices/" + deviceA + "/rotate-key")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/devices/" + deviceA + "/retire")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    /** Serials are global, so this is the check that stops one farm claiming another's hardware. */
    @Test
    void farmerBCannotRegisterACollarAlreadyOwnedByFarmerA() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNumber":"%s"}""".formatted(serialA)))
                .andExpect(status().isConflict());
    }

    @Test
    void farmerBCannotFitFarmerADeviceToTheirOwnCow() throws Exception {
        Long cowB = createCow(tokenB, "ISO-DEV-B");

        mockMvc.perform(put("/api/devices/" + deviceA + "/assign/" + cowB)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    /**
     * The device acts as the farm resolved from its own credentials, so a stolen
     * key cannot be pointed at somebody else's animal: there is no cow id in the
     * payload to redirect.
     */
    @Test
    void aDeviceWritesOnlyToTheAnimalItIsFittedTo() throws Exception {
        Long cowB = createCow(tokenB, "ISO-DEV-B");

        mockMvc.perform(post("/api/ingest/readings")
                        .header("X-Device-Serial", serialA)
                        .header("X-Device-Key", keyA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readings":[{"recordedAt":"%s","latitude":-23.9,"longitude":29.4,"accuracy":10}]}"""
                                .formatted(LocalDateTime.now().minusMinutes(5)
                                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                .andExpect(status().isOk());

        // Farm B's animal is untouched.
        MvcResult history = mockMvc.perform(get("/api/locations/cow/" + cowB + "/history")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(history.getResponse().getContentAsString()).get("data")).isEmpty();

        // Farm A's animal received it.
        MvcResult mine = mockMvc.perform(get("/api/locations/cow/" + cowA + "/history")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(mine.getResponse().getContentAsString()).get("data")).hasSize(1);
    }
}
