package com.megastore.inventoryservice.article;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Instant createdAt
) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(article.getId(), article.getSku(), article.getName(),
                article.getDescription(), article.getPrice(), article.getCreatedAt());
    }
}
