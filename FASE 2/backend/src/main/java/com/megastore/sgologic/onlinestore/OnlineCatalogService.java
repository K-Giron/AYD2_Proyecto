package com.megastore.sgologic.onlinestore;

import com.megastore.sgologic.inventory.InventoryLocationActivityService;
import com.megastore.sgologic.inventory.InventoryQueryService;
import com.megastore.sgologic.inventory.InventoryStockResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Catálogo de la tienda online: la web muestra solo lo que el
 * administrador publicó, con su imagen, precio y descripción. Publicar
 * exige imagen y precio; la disponibilidad sale siempre del inventario
 * real de la tienda online (CD-WEB).
 */
@Service
public class OnlineCatalogService {

    static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private final CatalogProductRepository productRepository;
    private final CatalogImageRepository imageRepository;
    private final OnlineReceiptRepository receiptRepository;
    private final InventoryQueryService inventoryQueryService;
    private final InventoryLocationActivityService activityService;
    private final OnlineStoreProperties properties;
    private final Clock clock;

    OnlineCatalogService(CatalogProductRepository productRepository, CatalogImageRepository imageRepository,
                         OnlineReceiptRepository receiptRepository,
                         InventoryQueryService inventoryQueryService, InventoryLocationActivityService activityService,
                         OnlineStoreProperties properties, Clock clock) {
        this.activityService = activityService;
        this.receiptRepository = receiptRepository;
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.inventoryQueryService = inventoryQueryService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CatalogProductResponse> adminList() {
        Map<UUID, InventoryStockResponse> stock = inventoryQueryService.getStockAtLocation(properties.getLocationCode());
        Map<UUID, Instant> images = imageStamps();
        return productRepository.findAllByOrderByNameAsc().stream()
                .map(product -> toAdmin(product, stock.get(product.getProductId()), images.get(product.getProductId())))
                .toList();
    }

    /**
     * Inventario de la tienda online: lo que hay en CD-WEB (con reservas) más
     * lo que viene en camino, incluso de artículos que aún no tienen ficha.
     */
    @Transactional(readOnly = true)
    public List<OnlineInventoryRow> inventory() {
        Map<UUID, InventoryStockResponse> stock = inventoryQueryService.getStockAtLocation(properties.getLocationCode());
        Map<UUID, Integer> inTransit = new java.util.HashMap<>();
        Map<UUID, OnlineReceipt> transitInfo = new java.util.LinkedHashMap<>();
        receiptRepository.findByStatusOrderByOccurredAtAsc(OnlineReceipt.Status.EN_TRANSITO).forEach(receipt -> {
            inTransit.merge(receipt.getProductId(), receipt.getQuantity(), Integer::sum);
            transitInfo.putIfAbsent(receipt.getProductId(), receipt);
        });
        Map<UUID, OnlineInventoryRow> rows = new java.util.LinkedHashMap<>();
        for (CatalogProduct product : productRepository.findAllByOrderByNameAsc()) {
            InventoryStockResponse s = stock.get(product.getProductId());
            rows.put(product.getProductId(), new OnlineInventoryRow(product.getProductId(), product.getSku(),
                    product.getName(), s == null ? 0 : s.quantityOnHand(), s == null ? 0 : s.quantityReserved(),
                    s == null ? 0 : s.quantityAvailable(), inTransit.getOrDefault(product.getProductId(), 0), true,
                    product.isPublished()));
        }
        transitInfo.forEach((productId, receipt) -> rows.putIfAbsent(productId, new OnlineInventoryRow(productId,
                receipt.getSku(), receipt.getName(), 0, 0, 0, inTransit.get(productId), false, false)));
        return List.copyOf(rows.values());
    }

    @Transactional(readOnly = true)
    public List<OnlineMovementRow> movements() {
        Map<UUID, CatalogProduct> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(CatalogProduct::getProductId, product -> product));
        return activityService.activityAt(properties.getLocationCode()).stream()
                .map(activity -> {
                    CatalogProduct product = products.get(activity.productId());
                    return new OnlineMovementRow(activity.kind(), activity.productId(),
                            product == null ? null : product.getSku(),
                            product == null ? activity.productId().toString() : product.getName(),
                            activity.quantity(), activity.reason(), activity.status(), activity.orderId(),
                            activity.occurredAt(), activity.expiresAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicProductResponse> publishedList() {
        Map<UUID, InventoryStockResponse> stock = inventoryQueryService.getStockAtLocation(properties.getLocationCode());
        Map<UUID, Instant> images = imageStamps();
        return productRepository.findByPublishedTrueOrderByNameAsc().stream()
                .map(product -> toPublic(product, stock.get(product.getProductId()), images.get(product.getProductId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicProductResponse published(UUID productId) {
        CatalogProduct product = productRepository.findById(productId)
                .filter(CatalogProduct::isPublished)
                .orElseThrow(() -> new OnlineStoreException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "El producto no está publicado en la tienda online."));
        InventoryStockResponse stock = inventoryQueryService.getStockAtLocation(properties.getLocationCode()).get(productId);
        return toPublic(product, stock, imageStamps().get(productId));
    }

    @Transactional
    public CatalogProductResponse update(UUID productId, UpdateCatalogProductRequest request) {
        CatalogProduct product = get(productId);
        product.edit(request.name().trim(), blankToNull(request.description()), blankToNull(request.category()),
                request.price(), clock.instant());
        return adminView(product);
    }

    @Transactional
    public CatalogProductResponse setImage(UUID productId, String contentType, byte[] data) {
        CatalogProduct product = get(productId);
        String type = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
        if (!IMAGE_TYPES.contains(type)) {
            throw new OnlineStoreException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_TYPE_NOT_SUPPORTED",
                    "La imagen debe ser JPG, PNG o WebP.");
        }
        if (data == null || data.length == 0 || data.length > MAX_IMAGE_BYTES) {
            throw new OnlineStoreException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE",
                    "La imagen debe pesar entre 1 byte y 2 MB.");
        }
        Instant now = clock.instant();
        imageRepository.findById(productId).ifPresentOrElse(
                image -> image.replace(type, data, now),
                () -> imageRepository.save(CatalogImage.of(productId, type, data, now)));
        imageRepository.flush();
        return adminView(product);
    }

    /** Sin imagen no puede seguir publicado: se despublica en el mismo paso. */
    @Transactional
    public CatalogProductResponse deleteImage(UUID productId) {
        CatalogProduct product = get(productId);
        imageRepository.deleteById(productId);
        imageRepository.flush();
        if (product.isPublished()) {
            product.unpublish(clock.instant());
        }
        return adminView(product);
    }

    @Transactional
    public CatalogProductResponse publish(UUID productId) {
        CatalogProduct product = get(productId);
        if (!imageRepository.existsById(productId)) {
            throw new OnlineStoreException(HttpStatus.CONFLICT, "IMAGE_REQUIRED",
                    "Para publicar " + product.getName() + " primero carga su imagen.");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OnlineStoreException(HttpStatus.CONFLICT, "PRICE_REQUIRED",
                    "Para publicar " + product.getName() + " define un precio mayor a cero.");
        }
        product.publish(clock.instant());
        return adminView(product);
    }

    @Transactional
    public CatalogProductResponse unpublish(UUID productId) {
        CatalogProduct product = get(productId);
        product.unpublish(clock.instant());
        return adminView(product);
    }

    @Transactional(readOnly = true)
    public CatalogImage image(UUID productId) {
        return imageRepository.findById(productId).orElseThrow(() -> new OnlineStoreException(
                HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "El producto no tiene imagen."));
    }

    private CatalogProduct get(UUID productId) {
        return productRepository.findById(productId).orElseThrow(() -> new OnlineStoreException(
                HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                "La tienda online no tiene ese producto: primero debe recibirlo por folio del almacén central."));
    }

    private CatalogProductResponse adminView(CatalogProduct product) {
        InventoryStockResponse stock = inventoryQueryService.getStockAtLocation(properties.getLocationCode())
                .get(product.getProductId());
        return toAdmin(product, stock, imageStamps().get(product.getProductId()));
    }

    private Map<UUID, Instant> imageStamps() {
        return imageRepository.findStamps().stream()
                .collect(Collectors.toMap(CatalogImageRepository.ImageStamp::getProductId,
                        CatalogImageRepository.ImageStamp::getUpdatedAt));
    }

    /** La URL cambia cuando cambia la imagen, así el navegador puede guardarla en caché sin mostrar una vieja. */
    private static String imageUrl(UUID productId, Instant stamp) {
        return stamp == null ? null : "/api/v1/online-catalog/" + productId + "/image?v=" + stamp.toEpochMilli();
    }

    private static CatalogProductResponse toAdmin(CatalogProduct product, InventoryStockResponse stock, Instant image) {
        return new CatalogProductResponse(product.getProductId(), product.getSku(), product.getName(),
                product.getDescription(), product.getCategory(), product.getPrice(), imageUrl(product.getProductId(), image),
                product.isPublished(), product.getPublishedAt(),
                stock == null ? 0 : stock.quantityOnHand(), stock == null ? 0 : stock.quantityReserved(),
                stock == null ? 0 : stock.quantityAvailable(), product.getUpdatedAt());
    }

    private static PublicProductResponse toPublic(CatalogProduct product, InventoryStockResponse stock, Instant image) {
        return new PublicProductResponse(product.getProductId(), product.getSku(), product.getName(),
                product.getDescription(), product.getCategory(), product.getPrice(),
                imageUrl(product.getProductId(), image), stock == null ? 0 : stock.quantityAvailable());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
