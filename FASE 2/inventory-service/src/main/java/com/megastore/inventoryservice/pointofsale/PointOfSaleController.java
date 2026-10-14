package com.megastore.inventoryservice.pointofsale;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points-of-sale")
public class PointOfSaleController {

    private final PointOfSaleService pointOfSaleService;

    public PointOfSaleController(PointOfSaleService pointOfSaleService) {
        this.pointOfSaleService = pointOfSaleService;
    }

    @GetMapping
    public List<PointOfSaleResponse> list() {
        return pointOfSaleService.listAll();
    }

    @PostMapping
    public ResponseEntity<PointOfSaleResponse> create(@Valid @RequestBody CreatePointOfSaleRequest request) {
        PointOfSale pointOfSale = pointOfSaleService.create(request.code(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(pointOfSaleService.getResponseById(pointOfSale.getId()));
    }
}
