package com.megastore.sgologic.onlinestore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface OnlineReceiptRepository extends JpaRepository<OnlineReceipt, String> {

    @Query("select max(r.occurredAt) from OnlineReceipt r")
    Optional<Instant> findLatestOccurredAt();

    List<OnlineReceipt> findTop200ByOrderByOccurredAtDesc();

    List<OnlineReceipt> findByStatusOrderByOccurredAtAsc(OnlineReceipt.Status status);

    @Query("select r from OnlineReceipt r where upper(r.shipmentFolio) = :folio or upper(r.folio) = :folio order by r.folio")
    List<OnlineReceipt> findByShipmentFolio(@Param("folio") String folio);

    /** 0 filas = ya la recibió otra confirmación: no se vuelve a ingresar al inventario. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OnlineReceipt r set r.status = com.megastore.sgologic.onlinestore.OnlineReceipt.Status.RECIBIDO, "
            + "r.receivedBy = :actor, r.receivedAt = :now "
            + "where r.folio = :folio and r.status = com.megastore.sgologic.onlinestore.OnlineReceipt.Status.EN_TRANSITO")
    int markReceived(@Param("folio") String folio, @Param("actor") String actor, @Param("now") Instant now);
}
