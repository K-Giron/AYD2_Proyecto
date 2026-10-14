package com.megastore.sgologic.credit.api;

import java.util.Optional;
import java.util.UUID;

/** Frontera idempotente usada por checkout y por la Saga. */
public interface CreditPort {

    CreditEvaluationResult evaluate(CreditEvaluationRequest request);

    CreditOperationResult requestCredit(CreditRequest request);

    Optional<CreditOperationResult> latestForOrder(UUID orderId);
}
