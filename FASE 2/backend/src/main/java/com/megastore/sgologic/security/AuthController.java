package com.megastore.sgologic.security;

import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emite tokens JWT para las cuentas de demostración (BB-07, ADR-013).
 * Las credenciales nunca se registran; una falla responde 401 con el
 * formato común de error.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;
    private final SecurityProperties properties;

    AuthController(AuthenticationManager authenticationManager, JwtTokenService tokenService,
                   SecurityProperties properties) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            // Solo se propagan las autoridades de rol; Spring Security 7
            // agrega FACTOR_PASSWORD como autoridad técnica de autenticación.
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .toList();
            UUID customerId = properties.getUsers().stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(authentication.getName()))
                    .findFirst()
                    .map(SecurityProperties.User::getCustomerId)
                    .orElse(null);
            String token = tokenService.issueToken(authentication.getName(), roles, customerId);
            return ResponseEntity.ok(new LoginResponse(
                    token, "Bearer", tokenService.expiresAt(), roles, customerId));
        } catch (AuthenticationException invalidCredentials) {
            String correlationId = String.valueOf(httpRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
            ApiError error = new ApiError("INVALID_CREDENTIALS", "Credenciales inválidas.",
                    Instant.now(), httpRequest.getRequestURI(), correlationId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
                         List<String> roles, UUID customerId) {
    }
}
