package com.northstar.crm.auth;

import com.northstar.crm.auth.dto.LoginRequest;
import com.northstar.crm.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login endpoint: checks credentials and returns a signed JWT (see openapi.yaml). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
            String role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))       // skip Spring's FACTOR_* authorities
                    .map(a -> a.replaceFirst("^ROLE_", ""))   // ROLE_AGENT -> AGENT
                    .findFirst()
                    .orElseThrow();
            return ResponseEntity.ok(new LoginResponse(tokens.issue(auth), "Bearer", auth.getName(), role));
        } catch (AuthenticationException e) {
            // Same message for an unknown user or a wrong password, so attackers can't tell which.
            // Never log the password or the token.
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            problem.setTitle("Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }
    }
}
