package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

/** Repositorio reactivo de Spring Data R2DBC para la entidad CardEntity. */
public interface CardSpringDataRepository extends R2dbcRepository<CardEntity, Long> {
    /** Busca una tarjeta por su identificador único (hash AES). */
    Mono<CardEntity> findByIdentifier(String identifier);
}
