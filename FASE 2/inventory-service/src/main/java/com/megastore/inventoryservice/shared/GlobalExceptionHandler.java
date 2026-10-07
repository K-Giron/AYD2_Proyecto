package com.megastore.inventoryservice.shared;

import com.megastore.inventoryservice.article.ArticleNotFoundException;
import com.megastore.inventoryservice.article.DuplicateSkuException;
import com.megastore.inventoryservice.finance.InvoiceAlreadyPaidException;
import com.megastore.inventoryservice.finance.InvoiceNotFoundException;
import com.megastore.inventoryservice.logistics.InvalidShipmentTransitionException;
import com.megastore.inventoryservice.logistics.ShipmentNotFoundException;
import com.megastore.inventoryservice.pointofsale.DuplicatePointOfSaleCodeException;
import com.megastore.inventoryservice.pointofsale.PointOfSaleNotFoundException;
import com.megastore.inventoryservice.posstock.InsufficientPosStockException;
import com.megastore.inventoryservice.stock.AdjustmentReasonRequiredException;
import com.megastore.inventoryservice.stock.InsufficientStockException;
import com.megastore.inventoryservice.storesync.InvalidStoreTransferException;
import com.megastore.inventoryservice.stock.PointOfSaleRequiredException;
import com.megastore.inventoryservice.warehouse.WarehouseNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ArticleNotFoundException.class)
    ResponseEntity<ApiError> handleArticleNotFound(ArticleNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(DuplicateSkuException.class)
    ResponseEntity<ApiError> handleDuplicateSku(DuplicateSkuException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_SKU", exception, request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", exception, request);
    }

    @ExceptionHandler(InsufficientPosStockException.class)
    ResponseEntity<ApiError> handleInsufficientPosStock(InsufficientPosStockException exception,
                                                         HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INSUFFICIENT_POS_STOCK", exception, request);
    }

    @ExceptionHandler(AdjustmentReasonRequiredException.class)
    ResponseEntity<ApiError> handleAdjustmentReason(AdjustmentReasonRequiredException exception,
                                                     HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "ADJUSTMENT_REASON_REQUIRED", exception, request);
    }

    @ExceptionHandler(InvalidStoreTransferException.class)
    ResponseEntity<ApiError> handleInvalidStoreTransfer(InvalidStoreTransferException exception,
                                                         HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_STORE_TRANSFER", exception, request);
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    ResponseEntity<ApiError> handleInvoiceNotFound(InvoiceNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(InvoiceAlreadyPaidException.class)
    ResponseEntity<ApiError> handleInvoiceAlreadyPaid(InvoiceAlreadyPaidException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVOICE_ALREADY_PAID", exception, request);
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    ResponseEntity<ApiError> handleShipmentNotFound(ShipmentNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(InvalidShipmentTransitionException.class)
    ResponseEntity<ApiError> handleInvalidShipmentTransition(InvalidShipmentTransitionException exception,
                                                               HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_SHIPMENT_TRANSITION", exception, request);
    }

    @ExceptionHandler(WarehouseNotFoundException.class)
    ResponseEntity<ApiError> handleWarehouseNotFound(WarehouseNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "WAREHOUSE_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(PointOfSaleNotFoundException.class)
    ResponseEntity<ApiError> handlePointOfSaleNotFound(PointOfSaleNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "POINT_OF_SALE_NOT_FOUND", exception, request);
    }

    @ExceptionHandler(DuplicatePointOfSaleCodeException.class)
    ResponseEntity<ApiError> handleDuplicatePointOfSaleCode(DuplicatePointOfSaleCodeException exception,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_POINT_OF_SALE_CODE", exception, request);
    }

    @ExceptionHandler(PointOfSaleRequiredException.class)
    ResponseEntity<ApiError> handlePointOfSaleRequired(PointOfSaleRequiredException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "POINT_OF_SALE_REQUIRED", exception, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, RuntimeException exception,
                                            HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, exception.getMessage(), Instant.now(), request.getRequestURI()));
    }
}
