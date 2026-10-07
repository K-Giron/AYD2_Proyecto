package com.megastore.inventoryservice.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    boolean existsBySku(String sku);

    Optional<Article> findBySku(String sku);

    List<Article> findAllByOrderByNameAsc();
}
