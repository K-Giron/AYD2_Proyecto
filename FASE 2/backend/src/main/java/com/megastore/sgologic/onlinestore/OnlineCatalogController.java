package com.megastore.sgologic.onlinestore;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Catálogo público de la web: solo lo publicado, con disponibilidad real. */
@RestController
@RequestMapping("/api/v1/online-catalog")
class OnlineCatalogController {

    private final OnlineCatalogService service;
    private final OnlineStoreProperties properties;

    OnlineCatalogController(OnlineCatalogService service, OnlineStoreProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    /** Ubicaciones físicas activas donde el cliente puede recoger su compra. */
    @GetMapping("/pickup-points")
    public List<OnlineStoreProperties.PickupPoint> pickupPoints() {
        return properties.getPickupPoints();
    }

    @GetMapping
    public List<PublicProductResponse> list() {
        return service.publishedList();
    }

    @GetMapping("/{productId}")
    public PublicProductResponse get(@PathVariable UUID productId) {
        return service.published(productId);
    }

    @GetMapping("/{productId}/image")
    public ResponseEntity<byte[]> image(@PathVariable UUID productId) {
        CatalogImage image = service.image(productId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                // La URL lleva ?v=<fecha de la imagen>: si la imagen cambia, cambia la URL.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(image.getData());
    }
}
