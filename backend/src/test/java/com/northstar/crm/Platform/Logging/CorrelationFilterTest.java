package com.northstar.crm.Platform.Logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationFilterTest {

    private final CorrelationFilter filter = new CorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesValidRequestIdAndRestoresPreviousMdcValue() throws Exception {
        String correlationId = "lab-request-001";
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelationFilter.HEADER_NAME, correlationId);
        MDC.put(CorrelationFilter.MDC_KEY, "previous-value");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertEquals(correlationId, MDC.get(CorrelationFilter.MDC_KEY));
            ((MockHttpServletResponse) servletResponse).setStatus(202);
        });

        assertEquals(correlationId, response.getHeader(CorrelationFilter.HEADER_NAME));
        assertEquals(202, response.getStatus());
        assertEquals("previous-value", MDC.get(CorrelationFilter.MDC_KEY));
    }

    @Test
    void generatesAndReturnsAnIdWhenRequestHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var idDuringRequest = new AtomicReference<String>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
            idDuringRequest.set(MDC.get(CorrelationFilter.MDC_KEY))
        );

        String responseId = response.getHeader(CorrelationFilter.HEADER_NAME);

        assertNotNull(responseId);
        assertEquals(responseId, idDuringRequest.get());
        assertNotNull(UUID.fromString(responseId));
        assertNull(MDC.get(CorrelationFilter.MDC_KEY));
    }

    @Test
    void replacesAnInvalidRequestIdWithGeneratedId() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelationFilter.HEADER_NAME, "not a valid id!");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
            assertNotNull(UUID.fromString(MDC.get(CorrelationFilter.MDC_KEY)))
        );

        String responseId = response.getHeader(CorrelationFilter.HEADER_NAME);

        assertNotNull(responseId);
        assertNotEquals("not a valid id!", responseId);
        assertNull(MDC.get(CorrelationFilter.MDC_KEY));
    }

    @Test
    void clearsMdcWhenRequestProcessingThrows() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelationFilter.HEADER_NAME, "request-with-error");

        assertThrows(IOException.class, () ->
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                assertEquals("request-with-error", MDC.get(CorrelationFilter.MDC_KEY));
                throw new IOException("Simulated downstream failure");
            })
        );

        assertNull(MDC.get(CorrelationFilter.MDC_KEY));
        assertEquals(
            "request-with-error",
            response.getHeader(CorrelationFilter.HEADER_NAME)
        );
    }
}