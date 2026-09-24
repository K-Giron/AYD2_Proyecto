package com.megastore.sgologic.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    Optional<CustomerOrder> findByCheckoutKey(String checkoutKey);
    Optional<CustomerOrder> findByCartId(UUID cartId);
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
