package com.megastore.sgologic.credit.api;

/** Puerto de entrada del historial del cliente proveniente del ERP (credit-history-v1). */
public interface CreditHistoryPort {

    String CONTRACT_VERSION = "credit-history-v1";

    /**
     * Incorpora los hechos del lote de forma idempotente y recalcula el perfil
     * de cada cliente afectado. Un hecho ya recibido se cuenta como duplicado y
     * no modifica ningún total.
     */
    CreditHistoryIngestResult ingest(CreditHistoryBatch batch);
}
