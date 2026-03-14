package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad de persistencia que mapea la tabla CARD en la base de datos.
 * Utiliza R2DBC (reactivo) con Spring Data.
 */
@Getter
@Setter
@Builder
@Table("CARD")
public class CardEntity {
    @Id
    private Long id;                  // Clave primaria auto-generada por la BD
    private String identifier;        // Identificador único (hash AES del PAN + fecha)
    private String maskedPan;         // PAN enmascarado
    private String cardholder;        // Nombre del titular
    private String nationalId;        // Cédula del titular
    private String type;              // Tipo: Crédito o Débito
    private String phone;             // Teléfono del titular
    private String status;            // Estado: CREADA, Enrolada, INACTIVA
    private Integer validationNumber; // Número de validación (1-100)
}
