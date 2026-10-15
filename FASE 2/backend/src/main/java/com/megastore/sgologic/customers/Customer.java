package com.megastore.sgologic.customers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, updatable = false, unique = true, length = 160)
    private String username;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "registered_channel", nullable = false, updatable = false, length = 24)
    private String registeredChannel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    protected Customer() {
        // Requerido por JPA.
    }

    static Customer register(String username, String fullName, String registeredChannel, Instant now) {
        return register(username, fullName, registeredChannel, null, now);
    }

    static Customer register(String username, String fullName, String registeredChannel,
                             String passwordHash, Instant now) {
        Customer customer = new Customer();
        customer.id = UUID.randomUUID();
        customer.username = normalize(username);
        customer.fullName = fullName.trim();
        customer.registeredChannel = registeredChannel;
        customer.createdAt = now;
        customer.passwordHash = passwordHash;
        return customer;
    }

    static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRegisteredChannel() { return registeredChannel; }
    public Instant getCreatedAt() { return createdAt; }
    String getPasswordHash() { return passwordHash; }
}
