package com.northstar.crm.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the crm: block in application.yaml. */
@ConfigurationProperties(prefix = "crm")
public record AuthProperties(Jwt jwt, Security security, Cors cors) {

    public record Jwt(String secret, Duration ttl) {}

    public record Security(String agentPassword, String adminPassword) {}

    public record Cors(String allowedOrigin) {}
}