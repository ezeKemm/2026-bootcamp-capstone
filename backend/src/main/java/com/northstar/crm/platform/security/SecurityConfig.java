package com.northstar.crm.platform.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.northstar.crm.auth.AuthProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** JWT security filter chain: login is public, everything else needs a Bearer token with the right role. */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorized = (req, res, ex) -> problem(res, 401, "Authentication required");
        AccessDeniedHandler forbidden = (req, res, ex) -> problem(res, 403, "You do not have permission to do that");

        http
                .csrf(csrf -> csrf.disable())                                   // stateless API with Bearer tokens, no cookies
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // CORS preflight
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/customers").permitAll()   // guest browse (backlog)
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers/*/activate").hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers/*/interactions").hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/*/interactions").hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/*").hasAnyRole("AGENT", "ADMIN")
                        .anyRequest().authenticated())                              // deny by default
                .oauth2ResourceServer(o -> o
                        .authenticationEntryPoint(unauthorized)                     // bad/expired token -> problem+json 401
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter())))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized)                     // no token -> 401
                        .accessDeniedHandler(forbidden));                           // wrong role -> 403
        return http.build();
    }

    /** Reads the "roles" claim and turns AGENT into ROLE_AGENT, so hasRole("AGENT") works. */
    private JwtAuthenticationConverter rolesConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecretKey jwtSecretKey(AuthProperties props) {
        return new SecretKeySpec(props.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // bcrypt
    }

    @Bean
    UserDetailsService users(AuthProperties props, PasswordEncoder encoder) {
        // Lab-only in-memory users (Lab 28). Swap for a users table later if needed.
        return new InMemoryUserDetailsManager(
                User.withUsername("agent1").password(encoder.encode(props.security().agentPassword())).roles("AGENT").build(),
                User.withUsername("admin1").password(encoder.encode(props.security().adminPassword())).roles("ADMIN").build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);   // Security 7: constructor takes the service
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(props.cors().allowedOrigin()));
        cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        cors.setExposedHeaders(List.of("X-Correlation-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private static void problem(HttpServletResponse res, int status, String title) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.getWriter().write("{\"title\":\"" + title + "\",\"status\":" + status + "}");
    }
}
