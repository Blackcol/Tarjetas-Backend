package com.prueba_be.tarjetas.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Modelo de dominio que representa una tarjeta de crédito o débito.
 * El identificador se genera a partir de un hash AES del PAN y la fecha actual.
 */
@Getter
@Builder
public class Card {
    private String id;               // Identificador único generado con hash AES (PAN + fecha)
    private String maskedPan;        // PAN enmascarado: primeros 6 + últimos 4 dígitos visibles
    private String cardholder;       // Nombre del titular de la tarjeta
    private String nationalId;       // Cédula del titular (10 a 15 caracteres)
    private String type;             // Tipo de tarjeta: "Crédito" o "Débito"
    private String phone;            // Teléfono del titular (10 dígitos)
    private CardStatus status;       // Estado actual: CREADA, Enrolada o INACTIVA
    private Integer validationNumber; // Número de validación aleatorio (1 a 100)
}
