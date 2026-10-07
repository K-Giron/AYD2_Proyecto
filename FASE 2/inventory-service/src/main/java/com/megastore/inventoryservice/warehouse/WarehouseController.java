package com.megastore.inventoryservice.warehouse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseRepository repository;

    public WarehouseController(WarehouseRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WarehouseResponse> list() {
        return repository.findAllByOrderByCodeAsc().stream().map(WarehouseResponse::from).toList();
    }
}
