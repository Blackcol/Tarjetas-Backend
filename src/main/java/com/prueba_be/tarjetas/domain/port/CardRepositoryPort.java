package com.prueba_be.tarjetas.domain.port;

import com.prueba_be.tarjetas.domain.model.Card;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida del dominio para persistencia de tarjetas.
 * Define las operaciones que la capa de aplicación necesita sin conocer la implementación.
 */
public interface CardRepositoryPort {
    /** Guarda o actualiza una tarjeta en el repositorio. */
    Mono<Card> save(Card card);

    /** Busca una tarjeta por su identificador único (hash AES). */
    Mono<Card> findById(String id);
}
