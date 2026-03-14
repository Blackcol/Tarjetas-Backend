package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

/** Repositorio reactivo de Spring Data R2DBC para la entidad de auditoría. */
public interface AuditSpringDataRepository extends R2dbcRepository<AuditEntity, Long> {
}
