package com.megastore.sgologic.customers;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(UUID id, String username, String fullName, String registeredChannel,
                               Instant createdAt) {

    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getUsername(), customer.getFullName(),
                customer.getRegisteredChannel(), customer.getCreatedAt());
    }
}
