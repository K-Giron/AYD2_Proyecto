package com.megastore.sgologic.logistics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** CDU-07, A1: el cliente cambia el método de entrega tras confirmar la compra. */
public record ChangeDeliveryMethodRequest(@NotNull DispatchOrigin origin, @NotBlank String destination) {
}
