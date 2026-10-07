package com.megastore.sgologic.customers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        Customer customer = customerService.register(request.username(), request.fullName(),
                request.channel() == null ? "WEB" : request.channel());
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
    }

    @GetMapping("/by-username")
    public CustomerResponse findByUsername(@RequestParam String username) {
        return CustomerResponse.from(customerService.findByUsername(username));
    }
}
