package com.megastore.sgologic.security;

import com.megastore.sgologic.customers.Customer;
import com.megastore.sgologic.customers.CustomerService;
import com.megastore.sgologic.customers.DuplicateCustomerUsernameException;
import com.megastore.sgologic.shared.http.ApiError;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final CustomerService customers;
    private final PasswordEncoder passwordEncoder;

    AuthController(AuthenticationManager authenticationManager, JwtTokenService tokenService,
                   SecurityProperties properties, CustomerService customers,
                   PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.properties = properties;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
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
            String fullName = customerId == null ? authentication.getName() : customers.findById(customerId)
                    .map(Customer::getFullName).orElse(authentication.getName());
            return ResponseEntity.ok(loginResponse(authentication.getName(), fullName, roles, customerId));
        } catch (AuthenticationException invalidCredentials) {
            return customers.authenticate(request.username(), request.password(), passwordEncoder)
                    .<ResponseEntity<?>>map(customer -> ResponseEntity.ok(loginResponse(
                            customer.getUsername(), customer.getFullName(), List.of("CLIENT"), customer.getId())))
                    .orElseGet(() -> error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                            "Credenciales inválidas.", httpRequest));
        }
    }

    /** Crea una identidad CLIENT persistente y devuelve su primer JWT. */
    @PostMapping("/register")
    ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        boolean reservedUsername = properties.getUsers().stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(request.email()));
        if (reservedUsername) {
            return error(HttpStatus.CONFLICT, "DUPLICATE_CUSTOMER_USERNAME",
                    "Ya existe una cuenta con ese correo.", httpRequest);
        }
        try {
            Customer customer = customers.registerWithCredentials(
                    request.email(), request.fullName(), passwordEncoder.encode(request.password()));
            return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse(
                    customer.getUsername(), customer.getFullName(), List.of("CLIENT"), customer.getId()));
        } catch (DuplicateCustomerUsernameException duplicate) {
            return error(HttpStatus.CONFLICT, "DUPLICATE_CUSTOMER_USERNAME",
                    "Ya existe una cuenta con ese correo.", httpRequest);
        }
    }

    private LoginResponse loginResponse(String username, String fullName, List<String> roles, UUID customerId) {
        String token = tokenService.issueToken(username, roles, customerId);
        return new LoginResponse(token, "Bearer", tokenService.expiresAt(), roles,
                customerId, username, fullName);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
        return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now(),
                request.getRequestURI(), correlationId));
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record RegisterRequest(
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
                         List<String> roles, UUID customerId, String username, String fullName) {
    }
}
