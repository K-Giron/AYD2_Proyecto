package com.megastore.sgologic.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CustomerContactRepository extends JpaRepository<CustomerContact, UUID> {

    List<CustomerContact> findByCustomerIdAndEnabledTrueAndVerifiedAtIsNotNullOrderByPreferenceRankAsc(
            UUID customerId);
}
