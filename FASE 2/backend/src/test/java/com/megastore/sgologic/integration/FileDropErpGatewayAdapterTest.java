package com.megastore.sgologic.integration;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileDropErpGatewayAdapterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T20:30:00Z"), ZoneOffset.UTC);
    private final UUID batchId = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000001");

    private FileDropErpGatewayAdapter adapter(SimulatorMode mode, Path dir) {
        return new FileDropErpGatewayAdapter(new SimulatedErpGatewayAdapter(mode, 0, 0.0), dir.toString(),
                "America/Guatemala", clock);
    }

    @Test
    void writesTheBatchAsAFileInTheErpInbox(@TempDir Path dir) throws Exception {
        ErpGatewayPort.DeliveryResult result = adapter(SimulatorMode.SUCCESS, dir)
                .deliver(batchId, ErpExportFormat.CSV, "recordType,saleId\nTRAILER,0");

        assertThat(result.accepted()).isTrue();
        assertThat(result.acknowledgement()).isEqualTo("ERP-FILE:ventas_20261006_143000_a1b2c3d4.csv");
        Path file = dir.resolve("ventas_20261006_143000_a1b2c3d4.csv");
        assertThat(Files.readString(file)).isEqualTo("recordType,saleId\nTRAILER,0");
        // Nunca queda el temporal a medias en la carpeta del ERP.
        try (var files = Files.list(dir)) {
            assertThat(files.map(path -> path.getFileName().toString())).containsExactly("ventas_20261006_143000_a1b2c3d4.csv");
        }
    }

    @Test
    void simulatedOutageStillTriggersRetriesAndWritesNothing(@TempDir Path dir) throws Exception {
        assertThatThrownBy(() -> adapter(SimulatorMode.OUTAGE, dir).deliver(batchId, ErpExportFormat.XML, "<Sales/>"))
                .isInstanceOf(ErpGatewayPort.ErpUnavailableException.class);
        try (var files = Files.list(dir)) {
            assertThat(files.count()).isZero();
        }
    }
}
