package com.megastore.storeservice.transfers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findBySyncStatusOrderByCreatedAtAsc(TransferSyncStatus syncStatus);

    List<Transfer> findTop200ByOrderByCreatedAtDesc();

    long countBySyncStatus(TransferSyncStatus syncStatus);

    @Query(value = "select nextval('seq_transfer')", nativeQuery = true)
    long nextTransferSequence();
}
