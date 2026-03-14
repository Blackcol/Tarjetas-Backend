package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia que mapea la tabla TRANSACTION en la base de datos.
 * Utiliza R2DBC (reactivo) con Spring Data.
 */
@Getter
@Setter
@Builder
@Table("TRANSACTION")
public class TransactionEntity {
    @Id
    private Long idTransaction;        // Clave primaria auto-generada por la BD
    private String referenceNumber;    // Número de referencia de la compra (6 dígitos)
    private String cardId;             // Identificador de la tarjeta asociada
    private BigDecimal totalAmount;    // Monto total de la compra
    private String purchaseAddress;    // Dirección de la compra
    private String status;             // Estado: Aprobada, Rechazada, Anulada
    private LocalDateTime creationDate; // Fecha y hora de creación
}
