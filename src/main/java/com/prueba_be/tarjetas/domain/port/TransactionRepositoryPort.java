package com.prueba_be.tarjetas.domain.port;

import com.prueba_be.tarjetas.domain.model.Transaction;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida del dominio para persistencia de transacciones.
 * Define las operaciones que la capa de aplicación necesita sin conocer la implementación.
 */
public interface TransactionRepositoryPort {
    /** Guarda o actualiza una transacción en el repositorio. */
    Mono<Transaction> save(Transaction transaction);

    /** Busca una transacción por número de referencia e identificador de tarjeta. */
    Mono<Transaction> findByReferenceNumberAndCardId(String referenceNumber, String cardId);
}
