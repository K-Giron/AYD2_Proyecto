package com.megastore.sgologic.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Enmascara datos de pago antes de que un valor llegue a un log (BB-07,
 * EaC-12). Cubre secuencias que parecen PAN (13 a 19 dígitos con separadores
 * opcionales) y campos CVV/CVC explícitos. Los UUID y las fechas ISO no se
 * alteran porque ningún segmento alcanza la longitud de un PAN.
 */
@Component
public class SensitiveDataSanitizer {

    private static final String MASK = "***";

    private static final Pattern CVV_PATTERN = Pattern.compile(
            "(?i)\\b(?:cvv2?|cvc2?|securitycode|security-code)\\b\\s*[:=]?\\s*\\d{3,4}");

    /**
     * 13 a 19 dígitos con separadores simples, sin consumir separadores
     * finales y sin tocar segmentos de un UUID (precedidos o seguidos por
     * guion o dígito).
     */
    private static final Pattern PAN_PATTERN =
            Pattern.compile("(?<![0-9-])\\d(?:[ -]?\\d){12,18}(?![0-9-])");

    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = CVV_PATTERN.matcher(value).replaceAll(MASK);
        return PAN_PATTERN.matcher(masked).replaceAll(MASK);
    }
}
