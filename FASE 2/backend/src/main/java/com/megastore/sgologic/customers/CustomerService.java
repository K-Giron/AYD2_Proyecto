package com.megastore.sgologic.customers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final Clock clock;

    public CustomerService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Registra al cliente desde cualquier canal (web o tienda física); el usuario es único. */
    @Transactional
    public Customer register(String username, String fullName, String registeredChannel) {
        return register(username, fullName, registeredChannel, null);
    }

    /** Registra una cuenta web con una credencial ya codificada. */
    @Transactional
    public Customer registerWithCredentials(String username, String fullName, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("La contraseña codificada es obligatoria.");
        }
        return register(username, fullName, "WEB", passwordHash);
    }

    private Customer register(String username, String fullName, String registeredChannel, String passwordHash) {
        String normalized = Customer.normalize(username);
        if (repository.existsByUsername(normalized)) {
            throw new DuplicateCustomerUsernameException(normalized);
        }
        try {
            return repository.saveAndFlush(Customer.register(
                    normalized, fullName, registeredChannel, passwordHash, clock.instant()));
        } catch (DataIntegrityViolationException concurrentRegistration) {
            throw new DuplicateCustomerUsernameException(normalized);
        }
    }

    /** Valida una cuenta web sin exponer el hash fuera del módulo customers. */
    @Transactional(readOnly = true)
    public Optional<Customer> authenticate(String username, String rawPassword, PasswordEncoder passwordEncoder) {
        String normalized = Customer.normalize(username);
        return repository.findByUsername(normalized)
                .filter(customer -> customer.getPasswordHash() != null)
                .filter(customer -> passwordEncoder.matches(rawPassword, customer.getPasswordHash()));
    }

    @Transactional(readOnly = true)
    /** Para otros módulos (p. ej. notificaciones): el cliente por id, si existe. */
    public java.util.Optional<Customer> findById(java.util.UUID customerId) {
        return repository.findById(customerId);
    }

    public Customer findByUsername(String username) {
        String normalized = Customer.normalize(username);
        return repository.findByUsername(normalized).orElseThrow(() -> new CustomerNotFoundException(normalized));
    }
}
