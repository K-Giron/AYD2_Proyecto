package com.megastore.storeservice.sync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SyncLogRepository extends JpaRepository<SyncLogEntry, UUID> {

    List<SyncLogEntry> findTop300ByOrderByOccurredAtDesc();
}
