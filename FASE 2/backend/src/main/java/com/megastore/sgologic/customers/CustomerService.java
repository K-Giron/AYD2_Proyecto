package com.megastore.sgologic.customers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

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
        String normalized = Customer.normalize(username);
        if (repository.existsByUsername(normalized)) {
            throw new DuplicateCustomerUsernameException(normalized);
        }
        try {
            return repository.saveAndFlush(Customer.register(normalized, fullName, registeredChannel, clock.instant()));
        } catch (DataIntegrityViolationException concurrentRegistration) {
            throw new DuplicateCustomerUsernameException(normalized);
        }
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
