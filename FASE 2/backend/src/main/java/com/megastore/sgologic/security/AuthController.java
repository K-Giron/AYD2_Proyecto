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

    AuthController(AuthenticationManager authenticationManager, JwtTokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(authority -> authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority)
                    .toList();
            String token = tokenService.issueToken(authentication.getName(), roles);
            return ResponseEntity.ok(new LoginResponse(token, "Bearer", tokenService.expiresAt(), roles));
        } catch (AuthenticationException invalidCredentials) {
            String correlationId = String.valueOf(httpRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
            ApiError error = new ApiError("INVALID_CREDENTIALS", "Credenciales inválidas.",
                    Instant.now(), httpRequest.getRequestURI(), correlationId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record LoginResponse(String accessToken, String tokenType, Instant expiresAt, List<String> roles) {
    }
}
