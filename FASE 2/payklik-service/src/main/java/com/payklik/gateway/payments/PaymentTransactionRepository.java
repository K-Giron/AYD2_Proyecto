package com.payklik.gateway.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByMerchantAndIdempotencyKey(String merchant, String idempotencyKey);

    List<PaymentTransaction> findTop300ByOrderByCreatedAtDesc();

    @Query(value = "SELECT nextval('seq_authorization_code')", nativeQuery = true)
    long nextAuthorizationSequence();
}
