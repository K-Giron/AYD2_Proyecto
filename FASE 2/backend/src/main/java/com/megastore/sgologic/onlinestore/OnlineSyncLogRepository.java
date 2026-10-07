package com.megastore.sgologic.onlinestore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface OnlineSyncLogRepository extends JpaRepository<OnlineSyncLogEntry, UUID> {

    List<OnlineSyncLogEntry> findTop300ByOrderByOccurredAtDesc();
}
