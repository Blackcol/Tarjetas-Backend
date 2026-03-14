package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

/** Repositorio reactivo de Spring Data R2DBC para la entidad TransactionEntity. */
public interface TransactionSpringDataRepository extends R2dbcRepository<TransactionEntity, Long> {
    /** Busca una transacción por número de referencia e identificador de tarjeta. */
    Mono<TransactionEntity> findByReferenceNumberAndCardId(String referenceNumber, String cardId);
}
