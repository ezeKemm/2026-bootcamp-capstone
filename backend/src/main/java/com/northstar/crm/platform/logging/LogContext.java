package com.northstar.crm.platform.logging;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Tags the rest of the current request's log lines with the customer and operation
 * (printed as cust= and op= by logback-spring.xml). CorrelationFilter removes both
 * when the request ends.
 *
 * Only pass IDs and fixed operation names here: never names, emails or note text.
 */
public final class LogContext {

    private LogContext() {
    }

    public static void customerOperation(UUID customerId, String operation) {
        MDC.put(CorrelationFilter.CUSTOMER_MDC_KEY, String.valueOf(customerId));
        MDC.put(CorrelationFilter.OPERATION_MDC_KEY, operation);
    }
}
