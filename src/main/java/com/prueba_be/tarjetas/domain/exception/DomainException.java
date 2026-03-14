package com.prueba_be.tarjetas.domain.exception;

/**
 * Excepción personalizada del dominio.
 * Incluye un código de respuesta (ej: "00", "01", "02") y un mensaje descriptivo.
 * Se utiliza para comunicar errores de negocio desde la capa de dominio/aplicación
 * hacia la capa de infraestructura (controllers).
 */
public class DomainException extends RuntimeException {
    private final String code; // Código de respuesta del negocio

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
