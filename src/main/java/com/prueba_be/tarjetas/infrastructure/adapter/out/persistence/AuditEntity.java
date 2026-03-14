package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia que mapea la tabla AUDIT en la base de datos.
 * Registra las operaciones que modifican datos: crear tarjeta, enrolar,
 * eliminar, crear transacción y anular transacción.
 */
@Getter
@Setter
@Builder
@Table("AUDIT")
public class AuditEntity {
    @Id
    private Long id;                    // Clave primaria auto-generada
    private String entityName;          // Nombre de la entidad afectada (CARD, TRANSACTION)
    private String entityId;            // Identificador de la entidad afectada
    private String operation;           // Operación realizada (INSERT, UPDATE)
    private LocalDateTime executionDate; // Fecha y hora de la operación
}
