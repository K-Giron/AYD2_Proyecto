package com.megastore.sgologic.notifications;

import org.springframework.stereotype.Component;

import com.megastore.sgologic.customers.Customer;
import com.megastore.sgologic.customers.CustomerService;

import java.time.Clock;
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
    private final CustomerService customerService;
    private final Clock clock;

    CustomerLookupService(OrderCustomerLinkRepository customerLinkRepository,
                          CustomerContactRepository contactRepository, CustomerService customerService, Clock clock) {
        this.customerLinkRepository = customerLinkRepository;
        this.contactRepository = contactRepository;
        this.customerService = customerService;
        this.clock = clock;
    }

    /** Para eventos de despacho/incidencia, que solo conocen {@code orderId}. */
    Optional<UUID> resolveCustomerIdForOrder(UUID orderId) {
        return customerLinkRepository.findById(orderId).map(OrderCustomerLink::getCustomerId);
    }

    /**
     * Contacto preferido del cliente. Si aún no tiene ninguno pero está en el
     * directorio de clientes con un correo como usuario (se registró en la web
     * o en tienda), ese correo pasa a ser su contacto de correo: así todo
     * cliente registrado recibe sus notificaciones sin un alta aparte.
     */
    Optional<CustomerContact> resolvePreferredContact(UUID customerId) {
        List<CustomerContact> contacts = contactRepository
                .findByCustomerIdAndEnabledTrueAndVerifiedAtIsNotNullOrderByPreferenceRankAsc(customerId);
        if (!contacts.isEmpty()) {
            return Optional.of(contacts.getFirst());
        }
        return customerService.findById(customerId)
                .map(Customer::getUsername)
                .filter(username -> username.contains("@"))
                .map(email -> contactRepository.save(CustomerContact.verified(customerId, NotificationChannel.EMAIL,
                        email, 1, clock.instant())));
    }
}
