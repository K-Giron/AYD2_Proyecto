package com.megastore.sgologic.credit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Consulta la explicación de una obligación y la decisión que la originó (EDS3-04). */
@Service
class CreditExplanationService {

    private final CreditOperationRepository operations;
    private final CreditDecisionRepository decisions;

    CreditExplanationService(CreditOperationRepository operations, CreditDecisionRepository decisions) {
        this.operations = operations;
        this.decisions = decisions;
    }

    @Transactional(readOnly = true)
    CreditOperationExplanation explain(UUID operationId) {
        CreditOperation operation = operations.findById(operationId)
                .orElseThrow(() -> new CreditOperationNotFoundException(operationId));
        CreditDecision decision = decisions.findById(operation.getDecisionId())
                .orElseThrow(() -> new CreditOperationNotFoundException(operationId));
        return new CreditOperationExplanation(toOperation(operation), toDecision(decision));
    }

    private CreditOperationExplanation.Operation toOperation(CreditOperation operation) {
        return new CreditOperationExplanation.Operation(operation.getId(), operation.getOrderId(),
                operation.getCustomerId(), operation.getStatus(), operation.getAmount(), operation.getCurrency(),
                operation.getApprovedLimit(), operation.getDueDate(), operation.getRuleVersion(),
                operation.getReasonCode(), operation.getCreatedAt(), operation.getCorrelationId());
    }

    private CreditOperationExplanation.Decision toDecision(CreditDecision decision) {
        return new CreditOperationExplanation.Decision(decision.getId(), decision.isEligible(),
                decision.getRequestedAmount(), decision.getApprovedLimit(), decision.getRuleVersion(),
                decision.getReasonCode(), decision.getInputsSnapshot(), decision.getEvaluatedAt(),
                decision.getCorrelationId());
    }
}
