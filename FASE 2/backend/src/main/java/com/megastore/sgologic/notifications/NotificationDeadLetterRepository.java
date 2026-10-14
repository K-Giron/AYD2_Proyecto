package com.megastore.sgologic.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface NotificationDeadLetterRepository extends JpaRepository<NotificationDeadLetter, UUID> {
}
