package com.northstar.crm.platform.logging;

import com.northstar.crm.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves logback-spring.xml is active: log lines carry the request's correlation ID
 * as corr=..., so support can search every line of one request.
 */
@SpringBootTest(properties = "crm.jwt.secret=test-only-secret-not-used-anywhere-real-32+")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class LogPatternIT {

    @Autowired MockMvc mvc;

    @Test
    void requestLogLineCarriesCorrelationIdFromHeader(CapturedOutput output) throws Exception {
        mvc.perform(get("/api/v1/public/customers")
                .header(CorrelationFilter.HEADER_NAME, "lab-request-001"))
            .andExpect(status().isOk());

        assertThat(output.getAll().lines()
                .filter(line -> line.contains("HTTP GET /api/v1/public/customers completed status=200")))
            .as("request log line uses the structured pattern")
            .anySatisfy(line -> assertThat(line)
                .contains("corr=lab-request-001")
                .contains(" cust=")
                .contains(" op=")
                .contains("CorrelationFilter"));
    }
}
