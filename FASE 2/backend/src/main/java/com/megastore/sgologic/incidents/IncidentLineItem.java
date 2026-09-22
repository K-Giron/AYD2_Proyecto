package com.megastore.sgologic.incidents;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class IncidentLineItem {

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected IncidentLineItem() {
        // Requerido por JPA.
    }

    public IncidentLineItem(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }
}
