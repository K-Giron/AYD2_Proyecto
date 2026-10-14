package com.logiexpress.carrier.operation;

import com.logiexpress.carrier.shared.CarrierException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class OperationService {

    private static final short SINGLETON = 1;

    private final OperationSettingsRepository repository;
    private final Clock clock;

    OperationService(OperationSettingsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OperationSettings current() {
        return repository.findById(SINGLETON).orElseThrow();
    }

    @Transactional
    public OperationSettings change(OperationMode mode, String updatedBy) {
        OperationSettings settings = current();
        settings.change(mode, updatedBy, clock.instant());
        return repository.save(settings);
    }

    /** Antes de atender a un comercio: fuera de servicio corta la llamada con 503. */
    public void rejectIfOutOfService() {
        if (current().getMode() == OperationMode.FUERA_DE_SERVICIO) {
            throw new CarrierException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                    "LogiExpress está fuera de servicio por mantenimiento. Intente más tarde.");
        }
    }

    /** En modo LENTO la respuesta al comercio se demora (la operación ya quedó hecha). */
    public void delayIfSlow() {
        OperationSettings settings = current();
        if (settings.getMode() != OperationMode.LENTO) {
            return;
        }
        try {
            Thread.sleep(settings.getSlowLatencyMs());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
