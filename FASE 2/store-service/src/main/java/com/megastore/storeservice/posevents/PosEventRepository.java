package com.megastore.storeservice.posevents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PosEventRepository extends JpaRepository<PosEvent, UUID> {

    List<PosEvent> findTop500ByOrderByReceivedAtDesc();
}
