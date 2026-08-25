package com.cowtrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end coverage of the JWT flow the web client depends on: register, log in,
 * call a protected endpoint with the issued token, and reject requests without one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REGISTER_BODY = """
            {"name":"Tshepo Maabane","email":"%s","password":"secret123","role":"farmer",
             "phone":"0800000000","farmName":"Green Pastures"}
            """;

    @Test
    void registerIssuesTokenAndAcceptsClientFieldNames() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.formatted("register@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("register@example.com"))
                // The client posts "name"; it must map onto fullName.
                .andExpect(jsonPath("$.user.fullName").value("Tshepo Maabane"))
                .andExpect(jsonPath("$.user.role").value("FARMER"))
                .andReturn();

        // Password must never come back in the response body.
        assertThat(result.getResponse().getContentAsString()).doesNotContain("secret123");
    }

    @Test
    void loginReturnsTokenUsableOnProtectedEndpoints() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.formatted("login@example.com")))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"secret123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = body.get("token").asText();

        mockMvc.perform(get("/api/cows").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/verify").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.formatted("wrongpw@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrongpw@example.com","password":"not-the-password"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsMissingAndGarbageTokens() throws Exception {
        mockMvc.perform(get("/api/cows"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/cows").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.formatted("dupe@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.formatted("dupe@example.com")))
                .andExpect(status().isConflict());
    }

    /**
     * The CRA dev server is a different origin, so preflight must succeed without a
     * token. This is the case the old @CrossOrigin-only setup got wrong.
     */
    @Test
    void corsPreflightFromDevServerIsAllowed() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", HttpMethod.POST.name())
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
