package com.megastore.storeservice.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StoreReceiptRepository extends JpaRepository<StoreReceipt, String> {

    @Query("select max(r.occurredAt) from StoreReceipt r")
    Optional<Instant> findLatestOccurredAt();

    List<StoreReceipt> findTop200ByOrderByOccurredAtDesc();

    List<StoreReceipt> findByStatusOrderByOccurredAtAsc(StoreReceipt.Status status);

    /** 0 filas = otra confirmación ya la recibió: no se vuelve a sumar. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StoreReceipt r set r.status = com.megastore.storeservice.inventory.StoreReceipt.Status.RECIBIDO, "
            + "r.receivedBy = :seller, r.appliedAt = :now "
            + "where r.folio = :folio and r.status = com.megastore.storeservice.inventory.StoreReceipt.Status.EN_TRANSITO")
    int markReceived(@Param("folio") String folio, @Param("seller") String seller, @Param("now") Instant now);

    /** Por folio de envío (SAL-…/TRA-…) o por el folio POS-ENT propio. */
    @Query("select r from StoreReceipt r where upper(r.originFolio) = :folio or upper(r.folio) = :folio order by r.folio")
    List<StoreReceipt> findByShipmentFolio(@Param("folio") String folio);
}
