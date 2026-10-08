package com.northstar.crm.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.northstar.crm.browsePublicCustomers.BrowsePublicCustomersController;
import com.northstar.crm.browsePublicCustomers.BrowsePublicCustomersService;
import com.northstar.crm.platform.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Proves the JWT filter chain (Lab 28): 200 / 400 / 401 / 403 paths, roles, and CORS.
 * Uses the real SecurityConfig and TokenService; no database or Docker needed.
 */
@WebMvcTest(
        controllers = {AuthController.class, BrowsePublicCustomersController.class},
        properties = {
            "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+",
            "crm.jwt.ttl=1h",
            "crm.security.agent-password=agent1",
            "crm.security.admin-password=admin1",
            "crm.cors.allowed-origin=http://localhost:4200"
        })
@Import({SecurityConfig.class, TokenService.class})
class SecurityPathTest {

    private static final String CUSTOMER = "/api/v1/customers/5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";
    private static final String INTERACTION_BODY = "{\"channel\":\"PHONE\",\"summary\":\"Called about renewal.\"}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtDecoder jwtDecoder;

    @MockitoBean
    BrowsePublicCustomersService browseService;

    // ---------- Login ----------

    @Test
    void agentCanLogInAndGetsTokenUsernameAndRole() throws Exception {
        mockMvc.perform(login("agent1", "agent1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("agent1"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void adminLoginReturnsAdminRole() throws Exception {
        mockMvc.perform(login("admin1", "admin1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void tokenCarriesUsernameAndOnlyRealRoles() throws Exception {
        Jwt jwt = jwtDecoder.decode(tokenFor("agent1", "agent1"));

        assertThat(jwt.getSubject()).isEqualTo("agent1");
        // Spring Security 7 adds FACTOR_* authorities; they must not leak into the token.
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("AGENT");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void wrongPasswordIs401WithProblemDetails() throws Exception {
        mockMvc.perform(login("agent1", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid username or password"));
    }

    @Test
    void unknownUserGetsTheSameMessageAsWrongPassword() throws Exception {
        mockMvc.perform(login("nobody", "agent1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid username or password"));
    }

    @Test
    void blankCredentialsAre400() throws Exception {
        mockMvc.perform(login("", ""))
                .andExpect(status().isBadRequest());
    }

    // ---------- 401: who are you? ----------

    @Test
    void protectedRouteWithoutTokenIs401() throws Exception {
        mockMvc.perform(get(CUSTOMER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication required"));
    }

    @Test
    void guestCannotRecordAnInteraction() throws Exception {
        mockMvc.perform(post(CUSTOMER + "/interaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INTERACTION_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIs401() throws Exception {
        mockMvc.perform(get(CUSTOMER).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 403: not allowed ----------

    @Test
    void adminCannotRecordAnInteraction() throws Exception {
        mockMvc.perform(post(CUSTOMER + "/interaction")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INTERACTION_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("You do not have permission to do that"));
    }

    // ---------- Allowed through security ----------
    // These endpoints aren't built yet, so "passed security" means anything except 401/403.

    @Test
    void realTokenFromLoginPassesSecurity() throws Exception {
        String token = tokenFor("agent1", "agent1");

        mockMvc.perform(get(CUSTOMER).header("Authorization", "Bearer " + token))
                .andExpect(passedSecurity());
    }

    @Test
    void agentCanRecordAnInteraction() throws Exception {
        mockMvc.perform(post(CUSTOMER + "/interaction")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INTERACTION_BODY))
                .andExpect(passedSecurity());
    }

    @Test
    void adminCanActivateACustomer() throws Exception {
        mockMvc.perform(post(CUSTOMER + "/activate")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(passedSecurity());
    }

    @Test
    void adminCanReadTheTimeline() throws Exception {
        mockMvc.perform(get(CUSTOMER + "/interactions")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(passedSecurity());
    }

    // ---------- Public + CORS ----------

    @Test
    void guestCanBrowsePublicCustomersWithoutToken() throws Exception {
        // Content is covered by BrowsePublicCustomersControllerTest; here we only prove no token is needed.
        mockMvc.perform(get("/api/v1/public/customers"))
                .andExpect(passedSecurity());
    }

    @Test
    void corsPreflightFromTheAngularDevServerIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    // ---------- helpers ----------

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String username, String password) {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private String tokenFor(String username, String password) throws Exception {
        String body = mockMvc.perform(login(username, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static ResultMatcher passedSecurity() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }
}
