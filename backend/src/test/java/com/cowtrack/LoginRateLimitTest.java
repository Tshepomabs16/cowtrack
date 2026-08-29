package com.cowtrack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The login endpoint must not give an attacker unlimited guesses: it throttles
 * requests from a single address and locks an account after repeated failures.
 *
 * <p>Each scenario uses a distinct client address so the counters do not leak
 * between tests or into the suite's ordinary login usage on {@code 127.0.0.1}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String LOGIN_BODY = """
            {"email":"%s","password":"not-the-password"}""";

    @Test
    void thirtyZerosAndOneAmenFromOneAddressGetsRateLimited() throws Exception {
        String ip = "203.0.113.11";
        // First 30 requests from one address: all genuinely unauthorised.
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteAddr(ip))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_BODY.formatted("spray-" + i + "@example.com")))
                    .andExpect(status().isUnauthorized());
        }
        // The 31st crosses the window limit.
        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("spray-third@example.com")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void accountLocksAfterFiveFailedLogins() throws Exception {
        String ip = "198.51.100.42";
        String email = "locked@example.com";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteAddr(ip))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_BODY.formatted(email)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted(email)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void aSuccessfulLoginClearsTheAccountsFailureStreak() throws Exception {
        // Register a real account, then burn the lockout budget without locking it.
        String ip = "198.51.100.99";
        mockMvc.perform(post("/api/auth/register")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rate Limit","email":"recovered@example.com",
                                 "password":"secret123","role":"farmer"}"""))
                .andExpect(status().isCreated());

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(remoteAddr(ip))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_BODY.formatted("recovered@example.com")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"recovered@example.com","password":"secret123"}"""))
                .andExpect(status().isOk());

        // The success wiped the count, so one more wrong guess is 401, not 429.
        mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddr(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("recovered@example.com")))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor remoteAddr(String addr) {
        return request -> {
            request.setRemoteAddr(addr);
            return request;
        };
    }
}