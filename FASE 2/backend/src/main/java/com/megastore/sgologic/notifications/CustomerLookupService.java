package com.megastore.sgologic.notifications;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolución compartida de cliente y contacto preferido, reutilizada por los
 * tres consumidores de eventos de negocio (orden, despacho e incidencia).
 */
@Component
class CustomerLookupService {

    private final OrderCustomerLinkRepository customerLinkRepository;
    private final CustomerContactRepository contactRepository;

    CustomerLookupService(OrderCustomerLinkRepository customerLinkRepository,
                          CustomerContactRepository contactRepository) {
        this.customerLinkRepository = customerLinkRepository;
        this.contactRepository = contactRepository;
    }

    /** Para eventos de despacho/incidencia, que solo conocen {@code orderId}. */
    Optional<UUID> resolveCustomerIdForOrder(UUID orderId) {
        return customerLinkRepository.findById(orderId).map(OrderCustomerLink::getCustomerId);
    }

    Optional<CustomerContact> resolvePreferredContact(UUID customerId) {
        List<CustomerContact> contacts = contactRepository
                .findByCustomerIdAndEnabledTrueAndVerifiedAtIsNotNullOrderByPreferenceRankAsc(customerId);
        return contacts.isEmpty() ? Optional.empty() : Optional.of(contacts.getFirst());
    }
}
