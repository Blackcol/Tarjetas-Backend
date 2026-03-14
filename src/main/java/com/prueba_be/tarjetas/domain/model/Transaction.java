package com.prueba_be.tarjetas.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de dominio que representa una transacción de compra.
 * Se asocia a una tarjeta mediante su identificador (cardId).
 */
@Getter
@Builder
public class Transaction {
    private String referenceNumber;    // Número de referencia de la compra (6 dígitos)
    private String cardId;             // Identificador de la tarjeta asociada
    private BigDecimal totalAmount;    // Monto total de la compra
    private String purchaseAddress;    // Dirección de la compra
    private TransactionStatus status;  // Estado: Aprobada, Rechazada o Anulada
    private LocalDateTime creationDate; // Fecha y hora de creación de la transacción
}
