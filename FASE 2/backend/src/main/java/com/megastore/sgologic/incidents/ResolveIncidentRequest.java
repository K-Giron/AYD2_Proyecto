package com.megastore.sgologic.incidents;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ResolveIncidentRequest(@NotNull IncidentStatus resolution, BigDecimal refundAmount) {
}
