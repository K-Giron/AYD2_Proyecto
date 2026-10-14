package com.megastore.sgologic.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Entrega del lote al ERP AS/400 como lo permite el enunciado (sin
 * modificar el ERP): un archivo CSV/XML en su carpeta de entrada
 * ({@code sgo.integrations.erp.outbox-dir}). Se escribe de forma atómica
 * (temporal y luego renombrado) para que el ERP nunca lea un archivo a
 * medias. Las condiciones de falla configurables del simulador BB-06
 * (caída, timeout, rechazo) se evalúan antes, para seguir pudiendo
 * demostrar los reintentos y la bandeja de fallidos.
 */
@Primary
@Component
class FileDropErpGatewayAdapter implements ErpGatewayPort {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SimulatedErpGatewayAdapter failureSimulation;
    private final Path outboxDir;
    private final Clock clock;
    private final ZoneId zone;

    FileDropErpGatewayAdapter(SimulatedErpGatewayAdapter failureSimulation,
                              @Value("${sgo.integrations.erp.outbox-dir:/var/sgo/erp-outbox}") String outboxDir,
                              @Value("${sgo.integrations.erp.timezone:America/Guatemala}") String timezone,
                              Clock clock) {
        this.failureSimulation = failureSimulation;
        this.outboxDir = Path.of(outboxDir);
        this.zone = ZoneId.of(timezone);
        this.clock = clock;
    }

    @Override
    public DeliveryResult deliver(UUID batchId, ErpExportFormat format, String payload) {
        DeliveryResult simulated = failureSimulation.deliver(batchId, format, payload);
        if (!simulated.accepted()) {
            return simulated;
        }
        String fileName = "ventas_" + STAMP.format(clock.instant().atZone(zone)) + "_"
                + batchId.toString().substring(0, 8) + (format == ErpExportFormat.CSV ? ".csv" : ".xml");
        try {
            Files.createDirectories(outboxDir);
            Path temporary = outboxDir.resolve("." + fileName + ".tmp");
            Files.writeString(temporary, payload, StandardCharsets.UTF_8);
            Files.move(temporary, outboxDir.resolve(fileName), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            throw new ErpUnavailableException("No se pudo dejar el archivo en la carpeta del ERP: " + failure.getMessage());
        }
        return new DeliveryResult(true, "ERP-FILE:" + fileName, null);
    }
}
