package com.megastore.inventoryservice.article;

import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ArticleService {

    private final ArticleRepository repository;
    private final StockService stockService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ArticleService(ArticleRepository repository, StockService stockService,
                           AuditLogService auditLogService, Clock clock) {
        this.repository = repository;
        this.stockService = stockService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public Article create(String sku, String name, String description, BigDecimal price) {
        if (repository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }
        Article article = repository.save(Article.create(sku, name, description, price, clock.instant()));
        stockService.initializeStock(article.getId());
        auditLogService.record("ARTICULO_CREADO", "Se creó el artículo " + sku + " (" + name + ").", null, null);
        return article;
    }

    @Transactional(readOnly = true)
    public List<Article> listAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Article getById(UUID articleId) {
        return repository.findById(articleId).orElseThrow(() -> new ArticleNotFoundException(articleId));
    }
}
