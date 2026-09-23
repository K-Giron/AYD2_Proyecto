package com.megastore.sgologic.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BB-07 / EaC-12: el sanitizador enmascara PAN y CVV antes de un log, sin
 * alterar identificadores ni fechas.
 */
class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();

    @Test
    void masksPanWithAndWithoutSeparators() {
        assertThat(sanitizer.mask("tarjeta=4111111111111111")).isEqualTo("tarjeta=***");
        assertThat(sanitizer.mask("pan 4111-1111-1111-1111 fin")).isEqualTo("pan *** fin");
        assertThat(sanitizer.mask("4111 1111 1111 1111")).isEqualTo("***");
    }

    @Test
    void masksCvvFields() {
        assertThat(sanitizer.mask("cvv=123")).isEqualTo("***");
        assertThat(sanitizer.mask("CVV: 4321 y cvc 999")).isEqualTo("*** y ***");
    }

    @Test
    void keepsUuidsTimestampsAndShortNumbers() {
        String uuid = "aaaaaaaa-0000-0000-0000-000000000001";
        assertThat(sanitizer.mask(uuid)).isEqualTo(uuid);
        assertThat(sanitizer.mask("2026-09-23T21:13:37.569938Z")).isEqualTo("2026-09-23T21:13:37.569938Z");
        assertThat(sanitizer.mask("orden 12345")).isEqualTo("orden 12345");
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(sanitizer.mask(null)).isNull();
        assertThat(sanitizer.mask("  ")).isEqualTo("  ");
    }
}
