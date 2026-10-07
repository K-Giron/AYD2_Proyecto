package com.logiexpress.carrier.operation;

/**
 * NORMAL; LENTO = la API responde tarde (procesa, pero contesta después del
 * tiempo de espera del comercio); FUERA_DE_SERVICIO = la API rechaza todo con 503.
 */
public enum OperationMode {
    NORMAL, LENTO, FUERA_DE_SERVICIO
}
