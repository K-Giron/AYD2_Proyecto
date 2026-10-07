package com.megastore.sgologic.onlinestore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID> {

    List<CatalogProduct> findAllByOrderByNameAsc();

    List<CatalogProduct> findByPublishedTrueOrderByNameAsc();
}
