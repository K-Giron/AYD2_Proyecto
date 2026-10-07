package com.megastore.inventoryservice.article;

import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.stock.StockService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceTest {

    private final ArticleRepository repository = mock(ArticleRepository.class);
    private final StockService stockService = mock(StockService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
    private final ArticleService service = new ArticleService(repository, stockService, auditLogService, clock);

    @Test
    void createSavesArticleAndInitializesStockAtZero() {
        when(repository.existsBySku("MS-001")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Article article = service.create("MS-001", "Camisa", "Camisa azul", new BigDecimal("199.90"));

        assertThat(article.getSku()).isEqualTo("MS-001");
        assertThat(article.getName()).isEqualTo("Camisa");
        assertThat(article.getPrice()).isEqualByComparingTo("199.90");
        verify(stockService).initializeStock(article.getId());
    }

    @Test
    void createThrowsWhenSkuAlreadyExists() {
        when(repository.existsBySku("MS-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create("MS-001", "Camisa", null, BigDecimal.TEN))
                .isInstanceOf(DuplicateSkuException.class);

        verify(repository, never()).save(any());
        verify(stockService, never()).initializeStock(any());
    }

    @Test
    void listAllDelegatesToRepository() {
        Article article = Article.create("MS-002", "Pantalón", null, BigDecimal.TEN, clock.instant());
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(article));

        assertThat(service.listAll()).containsExactly(article);
    }
}
