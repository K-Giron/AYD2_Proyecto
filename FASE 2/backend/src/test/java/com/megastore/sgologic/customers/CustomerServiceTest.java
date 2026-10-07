package com.megastore.sgologic.customers;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-04T10:00:00Z"), ZoneOffset.UTC);
    private final CustomerService service = new CustomerService(repository, clock);

    @Test
    void registerNormalizesUsernameAndAssignsAServerSideId() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = service.register("  Ana.Lopez@Correo.com ", "Ana López", "STORE");

        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getUsername()).isEqualTo("ana.lopez@correo.com");
        assertThat(customer.getRegisteredChannel()).isEqualTo("STORE");
    }

    @Test
    void registerRejectsAnExistingUsername() {
        when(repository.existsByUsername("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("ANA@correo.com", "Ana", "WEB"))
                .isInstanceOf(DuplicateCustomerUsernameException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentRegistrationOfTheSameUsernameIsReportedAsDuplicate() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_customers_username"));

        assertThatThrownBy(() -> service.register("ana@correo.com", "Ana", "WEB"))
                .isInstanceOf(DuplicateCustomerUsernameException.class);
    }

    @Test
    void findByUsernameIsCaseInsensitive() {
        Customer customer = Customer.register("ana@correo.com", "Ana", "WEB", clock.instant());
        when(repository.findByUsername("ana@correo.com")).thenReturn(Optional.of(customer));

        assertThat(service.findByUsername("Ana@Correo.com")).isSameAs(customer);
    }

    @Test
    void findByUsernameFailsForUnknownCustomer() {
        when(repository.findByUsername("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByUsername("nadie@correo.com"))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
