package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import com.prueba_be.tarjetas.domain.model.TransactionStatus;
import com.prueba_be.tarjetas.domain.model.Transaction;
import com.prueba_be.tarjetas.domain.port.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adaptador de persistencia para transacciones.
 * Implementa el puerto de salida del dominio usando Spring Data R2DBC.
 *
 * Maneja INSERT para nuevas transacciones y UPDATE para anulaciones,
 * buscando primero por número de referencia y tarjeta para evitar duplicados.
 */
@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionSpringDataRepository repository;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        // Si la transacción ya existe (misma referencia + tarjeta), se actualiza (ej: anulación)
        return repository.findByReferenceNumberAndCardId(transaction.getReferenceNumber(), transaction.getCardId())
                .flatMap(existing -> {
                    existing.setStatus(transaction.getStatus().getLabel());
                    existing.setTotalAmount(transaction.getTotalAmount());
                    existing.setPurchaseAddress(transaction.getPurchaseAddress());
                    return repository.save(existing);
                })
                .switchIfEmpty(insertNew(transaction))
                .map(this::toDomain);
    }

    /**
     * Inserta una nueva transacción en la base de datos.
     * El id es null para que Spring Data R2DBC haga INSERT automáticamente.
     */
    private Mono<TransactionEntity> insertNew(Transaction transaction) {
        TransactionEntity entity = TransactionEntity.builder()
                .referenceNumber(transaction.getReferenceNumber())
                .cardId(transaction.getCardId())
                .totalAmount(transaction.getTotalAmount())
                .purchaseAddress(transaction.getPurchaseAddress())
                .status(transaction.getStatus().getLabel())
                .creationDate(transaction.getCreationDate())
                .build();
        return repository.save(entity);
    }

    @Override
    public Mono<Transaction> findByReferenceNumberAndCardId(String referenceNumber, String cardId) {
        return repository.findByReferenceNumberAndCardId(referenceNumber, cardId)
                .map(this::toDomain);
    }

    private Transaction toDomain(TransactionEntity entity) {
        return Transaction.builder()
                .referenceNumber(entity.getReferenceNumber())
                .cardId(entity.getCardId())
                .totalAmount(entity.getTotalAmount())
                .purchaseAddress(entity.getPurchaseAddress())
                .status(TransactionStatus.fromLabel(entity.getStatus()))
                .creationDate(entity.getCreationDate())
                .build();
    }
}
