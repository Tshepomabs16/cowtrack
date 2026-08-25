package com.cowtrack;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the routing contract between the bundled web client and the API.
 *
 * <p>An unmatched API path must 404 rather than return the HTML shell: otherwise a
 * typo in a frontend endpoint yields a page of HTML that the client tries to parse
 * as JSON, which is painful to diagnose. It must also not report a 500, which is
 * what happened before {@code NoResourceFoundException} was handled explicitly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaRoutingTest {

    private static final String UNKNOWN_PATH = "/api/definitely-not-an-endpoint";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void signIn() throws Exception {
        // The in-memory database is shared across the tests in this class, so each
        // one registers under its own address rather than colliding on a 409.
        String email = "routes-" + UUID.randomUUID() + "@example.com";

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Route Tester","email":"%s",
                                 "password":"secret123","role":"farmer"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void unknownApiPathReturnsNotFoundForAnAuthenticatedCaller() throws Exception {
        mockMvc.perform(get(UNKNOWN_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownApiPathIsNeverAnsweredWithHtml() throws Exception {
        String contentType = mockMvc.perform(get(UNKNOWN_PATH)
                        .header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentType();

        if (contentType != null) {
            assertThat(contentType).doesNotContain("text/html");
        }
    }

    @Test
    void unknownApiPathStillRequiresAuthentication() throws Exception {
        // Unauthenticated callers get 401 and learn nothing about which paths exist.
        mockMvc.perform(get(UNKNOWN_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsRemainReachable() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/ping")).andExpect(status().isOk());
        mockMvc.perform(get("/api/info")).andExpect(status().isOk());
    }
}
