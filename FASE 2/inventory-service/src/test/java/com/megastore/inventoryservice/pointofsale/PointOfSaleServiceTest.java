package com.megastore.inventoryservice.pointofsale;

import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.warehouse.Warehouse;
import com.megastore.inventoryservice.warehouse.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointOfSaleServiceTest {

    private final PointOfSaleRepository repository = mock(PointOfSaleRepository.class);
    private final WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final PointOfSaleService service = new PointOfSaleService(repository, warehouseRepository, auditLogService);

    private static Warehouse warehouse(UUID id) throws Exception {
        var constructor = Warehouse.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Warehouse warehouse = constructor.newInstance();
        ReflectionTestUtils.setField(warehouse, "id", id);
        ReflectionTestUtils.setField(warehouse, "code", "ALM-CENTRAL");
        ReflectionTestUtils.setField(warehouse, "name", "Almacén Central");
        return warehouse;
    }

    @Test
    void createSavesPointOfSaleWithItsOwnNewWarehouse() {
        when(repository.existsByCode("POS-09")).thenReturn(false);
        when(warehouseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PointOfSale pointOfSale = service.create("POS-09", "Tienda Nueva");

        ArgumentCaptor<Warehouse> warehouseCaptor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(warehouseCaptor.capture());
        Warehouse createdWarehouse = warehouseCaptor.getValue();

        assertThat(pointOfSale.getCode()).isEqualTo("POS-09");
        assertThat(pointOfSale.getWarehouseId()).isEqualTo(createdWarehouse.getId());
        assertThat(createdWarehouse.getCode()).isEqualTo("ALM-POS-09");
        assertThat(createdWarehouse.getName()).isEqualTo("Almacén Tienda Nueva");
    }

    @Test
    void createThrowsWhenCodeAlreadyExists() {
        when(repository.existsByCode("POS-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create("POS-01", "Duplicada"))
                .isInstanceOf(DuplicatePointOfSaleCodeException.class);

        verify(repository, never()).save(any());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void getResponseByIdResolvesWarehouseDetails() throws Exception {
        UUID warehouseId = UUID.randomUUID();
        UUID pointOfSaleId = UUID.randomUUID();
        PointOfSale pointOfSale = PointOfSale.create("POS-01", "Tienda Centro", warehouseId);
        ReflectionTestUtils.setField(pointOfSale, "id", pointOfSaleId);
        when(repository.findById(pointOfSaleId)).thenReturn(Optional.of(pointOfSale));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse(warehouseId)));

        PointOfSaleResponse response = service.getResponseById(pointOfSaleId);

        assertThat(response.code()).isEqualTo("POS-01");
        assertThat(response.warehouseCode()).isEqualTo("ALM-CENTRAL");
    }

    @Test
    void getResponseByIdThrowsWhenPointOfSaleDoesNotExist() {
        UUID pointOfSaleId = UUID.randomUUID();
        when(repository.findById(pointOfSaleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResponseById(pointOfSaleId))
                .isInstanceOf(PointOfSaleNotFoundException.class);
    }
}
