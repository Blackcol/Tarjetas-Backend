package com.prueba_be.tarjetas.infrastructure.adapter.in.web;

import com.prueba_be.tarjetas.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la API REST.
 * Captura excepciones de dominio, validación y errores generales,
 * retornando respuestas JSON estandarizadas con códigos y mensajes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de negocio lanzadas desde la capa de dominio/aplicación.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException ex) {
        log.warn("Domain [{}] {}", ex.getCode(), ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("codigo", ex.getCode());
        response.put("mensaje", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /** Maneja errores de validación de campos (jakarta.validation). */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(WebExchangeBindException ex, ServerWebExchange exchange) {
        log.warn("Validation [03] {} - {}", ex.getAllErrors().size(), getRequestInfo(exchange));
        Map<String, Object> response = new HashMap<>();
        response.put("codigo", "03"); // Código personalizado para errores de validación
        response.put("mensaje", "Error de validación en los campos");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("errores", errors);
        return ResponseEntity.badRequest().body(response);
    }

    /** Maneja errores de base de datos */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex) {
        log.error("DB Error 04 [{}]: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(503).body(Map.of("codigo", "04", "mensaje", "Error de base de datos"));
    }

    /** Maneja cualquier excepción no controlada (error interno del servidor). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralExceptions(ServerWebExchange exchange, Exception ex) {
        log.error("ERROR 99 [{}] {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), getRequestInfo(exchange));
        Map<String, String> response = new HashMap<>();
        response.put("codigo", "99");
        response.put("mensaje", "Error interno del servidor");
        return ResponseEntity.internalServerError().body(response);
    }
    
    private String getRequestInfo(ServerWebExchange exchange) {
        if (exchange != null && exchange.getRequest() != null) {
            return String.format("Method=%s Path=%s", 
                exchange.getRequest().getMethod(), 
                exchange.getRequest().getPath().value());
        }
        return "Method=UNKNOWN Path=UNKNOWN";
    }
    
}
