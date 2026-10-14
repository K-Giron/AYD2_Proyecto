package com.megastore.sgologic.notifications;

import com.megastore.sgologic.shared.simulation.SimulatorMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Simulador BB-06 configurable del proveedor de SMS, correo y push. */
@Component("simulatedNotificationProvider")
class SimulatedNotificationProviderAdapter implements NotificationProviderPort {

    private final SimulatorMode mode;
    private final long latencyMs;
    private final double unavailableRate;

    SimulatedNotificationProviderAdapter(
            @Value("${sgo.simulators.notifications.mode:RANDOM}") SimulatorMode mode,
            @Value("${sgo.simulators.notifications.latency-ms:500}") long latencyMs,
            @Value("${sgo.simulators.notifications.failure-rate:0.05}") double unavailableRate) {
        this.mode = mode;
        this.latencyMs = Math.max(0L, latencyMs);
        this.unavailableRate = Math.max(0.0, Math.min(1.0, unavailableRate));
    }

    @Override
    public void send(NotificationChannel channel, String recipient, NotificationEventType eventType,
                     java.util.UUID referenceId) throws NotificationProviderUnavailableException {
        SimulatorMode effectiveMode = mode == SimulatorMode.RANDOM
                && ThreadLocalRandom.current().nextDouble() < unavailableRate
                ? SimulatorMode.OUTAGE : mode == SimulatorMode.RANDOM ? SimulatorMode.SUCCESS : mode;
        if (effectiveMode == SimulatorMode.LATENCY) {
            pause();
        }
        if (effectiveMode == SimulatorMode.OUTAGE || effectiveMode == SimulatorMode.TIMEOUT
                || effectiveMode == SimulatorMode.REJECTION) {
            throw new NotificationProviderUnavailableException(
                    "El proveedor de " + channel + " no está disponible para el evento " + eventType);
        }
    }

    private void pause() throws NotificationProviderUnavailableException {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new NotificationProviderUnavailableException(
                    "La simulacion del proveedor de notificaciones fue interrumpida.");
        }
    }
}
