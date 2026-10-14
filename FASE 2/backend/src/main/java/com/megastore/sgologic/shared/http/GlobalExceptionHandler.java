package com.megastore.sgologic.shared.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleValidation(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos inválidos.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(HttpMessageNotReadableException exception,
                                                         HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "El cuerpo JSON no es válido.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(MethodArgumentTypeMismatchException exception,
                                                            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Uno de los parámetros no es válido.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception,
                                                        HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "No tiene permisos para acceder a este recurso.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        Object requestCorrelationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        log.error("Error interno no manejado en {} {} (correlationId={})",
                request.getMethod(), request.getRequestURI(), requestCorrelationId, exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error interno inesperado.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        Object requestCorrelationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        String correlationId = requestCorrelationId == null ? "unavailable" : requestCorrelationId.toString();
        ApiError body = new ApiError(code, message, Instant.now(), request.getRequestURI(), correlationId);
        return ResponseEntity.status(status).body(body);
    }
}
