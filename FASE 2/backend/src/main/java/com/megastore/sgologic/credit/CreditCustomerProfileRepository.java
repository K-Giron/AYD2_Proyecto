package com.megastore.sgologic.credit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CreditCustomerProfileRepository extends JpaRepository<CreditCustomerProfile, UUID> {
}
