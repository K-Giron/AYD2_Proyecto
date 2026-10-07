package com.logiexpress.carrier.guides;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface GuideRepository extends JpaRepository<Guide, UUID> {

    Optional<Guide> findByMerchantAndMerchantReference(String merchant, String merchantReference);

    Optional<Guide> findByTrackingNumber(String trackingNumber);

    List<Guide> findTop200ByMerchantAndUpdatedAtAfterOrderByUpdatedAtAsc(String merchant, Instant after);

    List<Guide> findTop300ByOrderByUpdatedAtDesc();

    @Query(value = "SELECT nextval('seq_tracking_number')", nativeQuery = true)
    long nextTrackingSequence();
}
