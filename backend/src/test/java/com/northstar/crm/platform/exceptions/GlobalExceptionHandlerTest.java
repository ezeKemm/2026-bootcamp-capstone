package com.northstar.crm.platform.exceptions;

import com.northstar.crm.domain.CustomerId;
import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.domain.DomainException;
import com.northstar.crm.platform.logging.CorrelationFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final String MISSING_ID = "8a9c7d2e-0f22-41c7-b1ef-6d1f3ac4d2ce";

    private MockMvc mockMvc;

    /** Fake endpoints that throw each kind of error. */
    @RestController
    static class ThrowingController {
        record Body(@NotBlank String summary) {}

        @GetMapping("/test/not-found")
        void notFound() {
            throw new CustomerNotFoundException(CustomerId.of(MISSING_ID));
        }

        @GetMapping("/test/rule")
        void rule() {
            throw new DomainException("Only prospects can be activated.");
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody Body body) {
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("internal secret detail");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new CorrelationFilter())
                .build();
    }

    @Test
    void customerNotFoundIs404ProblemDetailWithCorrelationId() throws Exception {
        mockMvc.perform(get("/test/not-found").header(CorrelationFilter.HEADER_NAME, "lab-request-001"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Customer not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString(MISSING_ID)))
                .andExpect(jsonPath("$.correlationId").value("lab-request-001"));
    }

    @Test
    void businessRuleIs422() throws Exception {
        mockMvc.perform(get("/test/rule"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.title").value("Business rule violated"))
                .andExpect(jsonPath("$.detail").value("Only prospects can be activated."));
    }

    @Test
    void invalidBodyIs400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("summary"));
    }

    @Test
    void unexpectedErrorIs500AndHidesInternals() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Unexpected error"))
                .andExpect(jsonPath("$.detail").value(not(containsString("secret"))))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void unknownUrlIsStill404NotCaughtAs500() throws Exception {
        mockMvc.perform(get("/test/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
