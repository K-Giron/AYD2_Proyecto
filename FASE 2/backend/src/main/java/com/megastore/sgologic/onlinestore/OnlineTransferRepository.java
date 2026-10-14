package com.megastore.sgologic.onlinestore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface OnlineTransferRepository extends JpaRepository<OnlineTransfer, UUID> {

    List<OnlineTransfer> findTop200ByOrderByCreatedAtDesc();

    List<OnlineTransfer> findBySyncStatusOrderByCreatedAtAsc(OnlineTransfer.Status status);

    @Query(value = "select nextval('seq_online_transfer')", nativeQuery = true)
    long nextFolioSequence();
}
