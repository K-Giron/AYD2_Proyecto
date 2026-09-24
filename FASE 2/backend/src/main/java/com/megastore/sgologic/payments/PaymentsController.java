package com.megastore.sgologic.payments;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-attempts")
class PaymentsController {
    private final PaymentService service;

    PaymentsController(PaymentService service) { this.service = service; }

    @GetMapping("/{attemptId}")
    PaymentAttemptResponse get(@PathVariable UUID attemptId) {
        return PaymentAttemptResponse.from(service.getAttempt(attemptId));
    }

    @GetMapping
    List<PaymentAttemptResponse> forOrder(@RequestParam UUID orderId) {
        return service.getAttemptsForOrder(orderId).stream().map(PaymentAttemptResponse::from).toList();
    }
}
