package com.payklik.gateway.operation;

/**
 * NORMAL; RECHAZAR = el emisor declina todos los cobros; LENTO = autoriza,
 * pero responde después del tiempo de espera del comercio (debe conciliar);
 * FUERA_DE_SERVICIO = la API rechaza todo con 503.
 */
public enum OperationMode {
    NORMAL, RECHAZAR, LENTO, FUERA_DE_SERVICIO
}
