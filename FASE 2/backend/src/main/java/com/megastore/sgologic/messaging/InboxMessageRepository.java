package com.megastore.sgologic.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

interface InboxMessageRepository extends JpaRepository<InboxMessage, InboxMessageId> {
}
