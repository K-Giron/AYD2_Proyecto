package com.megastore.sgologic.audit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
class AuditAccessDeniedException extends RuntimeException {
    AuditAccessDeniedException() {
        super("El solicitante no está autorizado para consultar evidencia de auditoría.");
    }
}
