package com.megastore.sgologic.logistics.api;

import java.util.Optional;
import java.util.UUID;

/** Puerto público de LOG-01/02 para la Saga, idempotente por solicitud. */
public interface DispatchPort {
    DispatchResult createDispatch(DispatchRequest request);
    Optional<DispatchResult> latestForOrder(UUID orderId);
}
