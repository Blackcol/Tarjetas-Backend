package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import com.prueba_be.tarjetas.domain.model.CardStatus;
import com.prueba_be.tarjetas.domain.model.Card;
import com.prueba_be.tarjetas.domain.port.CardRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adaptador de persistencia para tarjetas.
 * Implementa el puerto de salida del dominio usando Spring Data R2DBC.
 *
 * Maneja INSERT para nuevas tarjetas y UPDATE para cambios de estado
 * (enrolamiento, eliminación lógica), buscando primero por identificador.
 */
@Component
@RequiredArgsConstructor
public class CardPersistenceAdapter implements CardRepositoryPort {

    private final CardSpringDataRepository repository;

    @Override
    public Mono<Card> save(Card card) {
        // Si viene con identificador del dominio, buscamos el registro para hacer UPDATE
        if (card.getId() != null) {
            return repository.findByIdentifier(card.getId())
                    .flatMap(existing -> {
                        existing.setMaskedPan(card.getMaskedPan());
                        existing.setCardholder(card.getCardholder());
                        existing.setNationalId(card.getNationalId());
                        existing.setType(card.getType());
                        existing.setPhone(card.getPhone());
                        existing.setStatus(card.getStatus().getLabel());
                        existing.setValidationNumber(card.getValidationNumber());
                        return repository.save(existing);
                    })
                    .switchIfEmpty(insertNew(card))
                    .map(this::toDomain);
        }
        return insertNew(card).map(this::toDomain);
    }

    /**
     * Inserta una nueva tarjeta en la base de datos.
     * El id es null para que Spring Data R2DBC haga INSERT automáticamente.
     */
    private Mono<CardEntity> insertNew(Card card) {
        CardEntity entity = CardEntity.builder()
                .identifier(card.getId())
                .maskedPan(card.getMaskedPan())
                .cardholder(card.getCardholder())
                .nationalId(card.getNationalId())
                .type(card.getType())
                .phone(card.getPhone())
                .status(card.getStatus().getLabel())
                .validationNumber(card.getValidationNumber())
                .build();
        // id es null → Spring Data R2DBC hace INSERT automáticamente
        return repository.save(entity);
    }

    @Override
    public Mono<Card> findById(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(this::toDomain);
    }

    /** Convierte una entidad de persistencia al modelo de dominio. */
    private Card toDomain(CardEntity entity) {
        return Card.builder()
                .id(entity.getIdentifier())
                .maskedPan(entity.getMaskedPan())
                .cardholder(entity.getCardholder())
                .nationalId(entity.getNationalId())
                .type(entity.getType())
                .phone(entity.getPhone())
                .status(CardStatus.fromLabel(entity.getStatus()))
                .validationNumber(entity.getValidationNumber())
                .build();
    }
}
