package com.logiexpress.carrier.guides;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface GuideEventRepository extends JpaRepository<GuideEvent, UUID> {

    List<GuideEvent> findByGuideIdInOrderByOccurredAtAsc(Collection<UUID> guideIds);
}
