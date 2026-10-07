package com.logiexpress.carrier.guides;

import com.logiexpress.carrier.shared.CarrierException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guides")
public class Guide {

    @Id
    private UUID id;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private String merchant;

    @Column(name = "merchant_reference", nullable = false)
    private String merchantReference;

    @Column(name = "order_reference")
    private String orderReference;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(name = "pickup_point")
    private String pickupPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuideStatus status;

    private String courier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Guide() {
    }

    static Guide issue(String trackingNumber, String merchant, String merchantReference, String orderReference,
                       String origin, String destination, String pickupPoint, Instant now) {
        Guide guide = new Guide();
        guide.id = UUID.randomUUID();
        guide.trackingNumber = trackingNumber;
        guide.merchant = merchant;
        guide.merchantReference = merchantReference;
        guide.orderReference = orderReference;
        guide.origin = origin;
        guide.destination = destination;
        guide.pickupPoint = pickupPoint == null || pickupPoint.isBlank() ? null : pickupPoint.trim();
        guide.status = GuideStatus.CREADA;
        guide.createdAt = now;
        guide.updatedAt = now;
        return guide;
    }

    /** El mensajero recoge el paquete en el origen. */
    void pickUp(String courier, Instant now) {
        if (status != GuideStatus.CREADA) {
            throw new CarrierException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "La guía " + trackingNumber + " ya fue recolectada (estado " + status + ").");
        }
        this.courier = courier;
        this.status = GuideStatus.EN_TRANSITO;
        this.updatedAt = now;
    }

    /** Entrega: al cliente en su domicilio, o en la tienda de retiro si la guía la indica. */
    GuideStatus deliver(String courier, Instant now) {
        if (status != GuideStatus.EN_TRANSITO) {
            throw new CarrierException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Solo se entrega una guía en tránsito; la " + trackingNumber + " está " + status + ".");
        }
        this.courier = courier;
        this.status = pickupPoint == null ? GuideStatus.ENTREGADA : GuideStatus.EN_PUNTO_DE_RETIRO;
        this.updatedAt = now;
        return status;
    }

    public UUID getId() { return id; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getMerchant() { return merchant; }
    public String getMerchantReference() { return merchantReference; }
    public String getOrderReference() { return orderReference; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getPickupPoint() { return pickupPoint; }
    public GuideStatus getStatus() { return status; }
    public String getCourier() { return courier; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
