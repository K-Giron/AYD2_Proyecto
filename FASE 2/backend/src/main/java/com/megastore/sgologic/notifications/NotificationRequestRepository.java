package com.megastore.sgologic.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificationRequestRepository extends JpaRepository<NotificationRequest, UUID> {

    List<NotificationRequest> findBySourceAggregateIdOrderByCreatedAtAsc(UUID sourceAggregateId);
}
