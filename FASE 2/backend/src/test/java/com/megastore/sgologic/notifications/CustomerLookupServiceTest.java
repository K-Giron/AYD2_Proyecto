package com.megastore.sgologic.notifications;

import com.megastore.sgologic.customers.Customer;
import com.megastore.sgologic.customers.CustomerService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerLookupServiceTest {

    private final OrderCustomerLinkRepository links = mock(OrderCustomerLinkRepository.class);
    private final CustomerContactRepository contacts = mock(CustomerContactRepository.class);
    private final CustomerService customers = mock(CustomerService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC);
    private final CustomerLookupService service = new CustomerLookupService(links, contacts, customers, clock);

    @Test
    void registeredCustomerWithoutContactGetsTheirEmailAsContact() {
        UUID customerId = UUID.randomUUID();
        Customer customer = mock(Customer.class);
        when(customer.getUsername()).thenReturn("ana@correo.com");
        when(contacts.findByCustomerIdAndEnabledTrueAndVerifiedAtIsNotNullOrderByPreferenceRankAsc(customerId))
                .thenReturn(List.of());
        when(customers.findById(customerId)).thenReturn(Optional.of(customer));
        when(contacts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<CustomerContact> contact = service.resolvePreferredContact(customerId);

        assertThat(contact).isPresent();
        assertThat(contact.get().getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(contact.get().getDestinationToken()).isEqualTo("ana@correo.com");
    }

    @Test
    void unknownCustomerStillHasNoContact() {
        UUID customerId = UUID.randomUUID();
        when(contacts.findByCustomerIdAndEnabledTrueAndVerifiedAtIsNotNullOrderByPreferenceRankAsc(customerId))
                .thenReturn(List.of());
        when(customers.findById(customerId)).thenReturn(Optional.empty());

        assertThat(service.resolvePreferredContact(customerId)).isEmpty();
        verify(contacts, never()).save(any());
    }
}
