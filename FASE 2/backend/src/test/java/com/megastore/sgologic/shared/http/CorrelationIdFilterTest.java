package com.megastore.sgologic.shared.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void reusesAValidIncomingCorrelationIdAndReturnsIt() throws Exception {
        String incoming = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, incoming);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observedInChain = new AtomicReference<>();

        filter.doFilter(request, response, (currentRequest, currentResponse) ->
                observedInChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertThat(observedInChain.get()).isEqualTo(incoming);
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(incoming);
        assertThat(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME)).isEqualTo(incoming);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesAndReturnsAUuidWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
        });

        String generated = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(generated).isNotBlank();
        assertThatCode(() -> UUID.fromString(generated)).doesNotThrowAnyException();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesAnInvalidIncomingCorrelationIdWithoutEchoingIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
        });

        String generated = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(generated).isNotEqualTo("not-a-uuid");
        assertThatCode(() -> UUID.fromString(generated)).doesNotThrowAnyException();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void clearsMdcEvenWhenTheRequestFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (currentRequest, currentResponse) -> {
                    throw new IllegalStateException("controlled test failure");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("controlled test failure");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
