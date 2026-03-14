package com.prueba_be.tarjetas.application.usecase;

import com.prueba_be.tarjetas.domain.exception.DomainException;
import com.prueba_be.tarjetas.domain.model.CardStatus;
import com.prueba_be.tarjetas.domain.model.TransactionStatus;
import com.prueba_be.tarjetas.domain.model.Transaction;
import com.prueba_be.tarjetas.domain.port.CardRepositoryPort;
import com.prueba_be.tarjetas.domain.port.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Caso de uso para la gestión de transacciones de compra.
 * Contiene la lógica de negocio para crear y anular transacciones.
 */
@Service
@RequiredArgsConstructor
public class TransactionUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;

    @Value("${tarjetas.transaction.annul-time-limit-minutes:5}")
    private int annulTimeLimitMinutes;

    /**
     * Crea una nueva transacción de compra.
     * Valida que la tarjeta exista y esté en estado Enrolada antes de aprobar la compra.
     *
     * @param identifier     identificador de la tarjeta (hash AES)
     * @param referenceNumber número de referencia de la compra (6 dígitos)
     * @param totalAmount    monto total de la compra
     * @param purchaseAddress dirección de la compra
     * @return Mono con la transacción aprobada
     */
    public Mono<Transaction> createTransaction(String identifier, String referenceNumber, BigDecimal totalAmount, String purchaseAddress) {
        return cardRepositoryPort.findById(identifier)
                .switchIfEmpty(Mono.error(new DomainException("01", "Tarjeta no existe")))
                .flatMap(card -> {
                    if (card.getStatus() != CardStatus.ENROLLED) {
                        return Mono.error(new DomainException("02", "Tarjeta no enrolada"));
                    }

                    Transaction transaction = Transaction.builder()
                            .referenceNumber(referenceNumber)
                            .cardId(card.getId())
                            .totalAmount(totalAmount)
                            .purchaseAddress(purchaseAddress)
                            .status(TransactionStatus.APPROVED)
                            .creationDate(LocalDateTime.now())
                            .build();

                    return transactionRepositoryPort.findByReferenceNumberAndCardId(referenceNumber, card.getId())
                            .flatMap(tx -> Mono.<Transaction>error(new DomainException("04", "Transacción duplicada")))
                            .switchIfEmpty(transactionRepositoryPort.save(transaction));
                });
    }

    /**
     * Anula una transacción de compra existente.
     * Solo se puede anular si se realizó hace menos de 5 minutos,
     * no ha sido anulada previamente y el monto coincide.
     *
     * @param cardIdentifier  identificador de la tarjeta (hash AES)
     * @param referenceNumber número de referencia de la transacción
     * @param totalAmount     monto total de la compra (debe coincidir con el original)
     * @return Mono con la transacción anulada
     */
    public Mono<Transaction> annulTransaction(String cardIdentifier, String referenceNumber, BigDecimal totalAmount) {
        return transactionRepositoryPort.findByReferenceNumberAndCardId(referenceNumber, cardIdentifier)
                .switchIfEmpty(Mono.error(new DomainException("01", "Número de referencia inválido")))
                .flatMap(transaction -> {
                    if (transaction.getStatus() == TransactionStatus.ANNULLED) {
                        return Mono.error(new DomainException("02", "No se puede anular transacción (Ya anulada)"));
                    }

                    // Validar que no hayan pasado más de 5 minutos desde la creación
                    if (transaction.getCreationDate().plusMinutes(annulTimeLimitMinutes).isBefore(LocalDateTime.now())) {
                        return Mono.error(new DomainException("02", "No se puede anular transacción (Tiempo expirado)"));
                    }

                    // Validar que el monto coincida con la transacción original
                    if (transaction.getTotalAmount().compareTo(totalAmount) != 0) {
                        return Mono.error(new DomainException("02", "No se puede anular transacción (Monto no coincide)"));
                    }

                    Transaction annulled = Transaction.builder()
                            .referenceNumber(transaction.getReferenceNumber())
                            .cardId(transaction.getCardId())
                            .totalAmount(transaction.getTotalAmount())
                            .purchaseAddress(transaction.getPurchaseAddress())
                            .status(TransactionStatus.ANNULLED)
                            .creationDate(transaction.getCreationDate()) // Se mantiene la fecha original
                            .build();

                    return transactionRepositoryPort.save(annulled);
                });
    }
}
