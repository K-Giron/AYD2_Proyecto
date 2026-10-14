package com.megastore.storeservice.inventory;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreArticleRepository extends JpaRepository<StoreArticle, UUID> {

    Optional<StoreArticle> findBySkuIgnoreCase(String sku);

    List<StoreArticle> findByNameIgnoreCase(String name);

    List<StoreArticle> findAllByOrderByNameAsc();

    /** Búsqueda manual en caja: el texto puede estar en el código, el nombre o la descripción. */
    @Query("select a from StoreArticle a where lower(a.sku) like :pattern or lower(a.name) like :pattern "
            + "or lower(coalesce(a.description, '')) like :pattern order by a.name")
    List<StoreArticle> search(@Param("pattern") String pattern, Limit limit);
}
