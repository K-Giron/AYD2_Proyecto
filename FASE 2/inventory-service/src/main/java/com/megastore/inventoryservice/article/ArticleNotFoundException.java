package com.megastore.inventoryservice.article;

import java.util.UUID;

public class ArticleNotFoundException extends RuntimeException {

    public ArticleNotFoundException(UUID articleId) {
        super("No existe un artículo con id " + articleId);
    }
}
