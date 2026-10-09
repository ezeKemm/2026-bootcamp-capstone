package com.northstar.crm.platform.exceptions;

import com.northstar.crm.domain.CustomerNotFoundException;
import com.northstar.crm.domain.DomainException;
import com.northstar.crm.platform.logging.CorrelationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns exceptions into RFC 9457 Problem Details (application/problem+json),
 * matching the ProblemDetails schema in openapi.yaml.
 * Extending ResponseEntityExceptionHandler means Spring's own errors
 * (unknown URL, wrong method, bad JSON, bad query params) also come back as Problem Details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final HttpStatusCode UNPROCESSABLE = HttpStatusCode.valueOf(422);

    /** 404: the customer id doesn't exist. */
    @ExceptionHandler(CustomerNotFoundException.class)
    ResponseEntity<Object> handleCustomerNotFound(CustomerNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Customer not found");
        return createResponseEntity(problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    /** 422: request was fine, but it breaks a business rule (e.g. activating a non-prospect). */
    @ExceptionHandler(DomainException.class)
    ResponseEntity<Object> handleBusinessRule(DomainException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(UNPROCESSABLE, ex.getMessage());
        problem.setTitle("Business rule violated");
        return createResponseEntity(problem, new HttpHeaders(), UNPROCESSABLE, request);
    }

    /** 400: @Valid request body failed; list each bad field so the form can show it. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "One or more fields are invalid.");
        problem.setTitle("Validation failed");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid")))
                .toList();
        problem.setProperty("errors", errors);
        return createResponseEntity(problem, headers, status, request);
    }

    /** 500: anything we didn't expect. Log the details, never send them to the client. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Quote the correlation ID if you report this.");
        problem.setTitle("Unexpected error");
        return createResponseEntity(problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /** Every Problem Detail leaving this class gets the request's correlation ID. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            String correlationId = MDC.get(CorrelationFilter.MDC_KEY);
            if (correlationId != null) {
                problem.setProperty("correlationId", correlationId);
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}