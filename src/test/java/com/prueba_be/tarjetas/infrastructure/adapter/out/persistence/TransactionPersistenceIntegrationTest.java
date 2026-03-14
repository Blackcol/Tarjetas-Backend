package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import com.prueba_be.tarjetas.domain.model.Transaction;
import com.prueba_be.tarjetas.domain.model.TransactionStatus;
import com.prueba_be.tarjetas.domain.model.Card;
import com.prueba_be.tarjetas.domain.model.CardStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test de integración contra H2 real.
 * Verifica que la persistencia de transacciones funcione correctamente.
 *
 * Separado de los tests unitarios con Mockito.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionPersistenceIntegrationTest {

    @Autowired
    private TransactionPersistenceAdapter transactionPersistenceAdapter;

    @Autowired
    private CardPersistenceAdapter cardPersistenceAdapter;

    @BeforeEach
    void setUp() {
        // Insertar una tarjeta para el FK de las transacciones
        Card card = Card.builder()
                .id("card-for-txn-test")
                .maskedPan("999999******9999")
                .cardholder("Test User")
                .nationalId("5050505050")
                .type("Crédito")
                .phone("3005555555")
                .status(CardStatus.ENROLLED)
                .validationNumber(10)
                .build();

        cardPersistenceAdapter.save(card).block();
    }

    @Test
    void save_NewTransaction_ShouldInsertInDatabase() {
        Transaction txn = Transaction.builder()
                .referenceNumber("REF-001")
                .cardId("card-for-txn-test")
                .totalAmount(new BigDecimal("150000.0000"))
                .purchaseAddress("Calle 123")
                .status(TransactionStatus.APPROVED)
                .creationDate(LocalDateTime.now())
                .build();

        StepVerifier.create(transactionPersistenceAdapter.save(txn))
                .expectNextMatches(saved ->
                        saved.getReferenceNumber().equals("REF-001") &&
                        saved.getCardId().equals("card-for-txn-test") &&
                        saved.getStatus() == TransactionStatus.APPROVED
                )
                .verifyComplete();
    }

    @Test
    void findByReferenceNumberAndCardId_ShouldReturnTransaction() {
        Transaction txn = Transaction.builder()
                .referenceNumber("REF-002")
                .cardId("card-for-txn-test")
                .totalAmount(new BigDecimal("50000.0000"))
                .purchaseAddress("Av Principal 456")
                .status(TransactionStatus.APPROVED)
                .creationDate(LocalDateTime.now())
                .build();

        StepVerifier.create(
                transactionPersistenceAdapter.save(txn)
                        .then(transactionPersistenceAdapter.findByReferenceNumberAndCardId("REF-002", "card-for-txn-test"))
        )
                .expectNextMatches(found ->
                        found.getReferenceNumber().equals("REF-002") &&
                        found.getTotalAmount().compareTo(new BigDecimal("50000.0000")) == 0
                )
                .verifyComplete();
    }

    @Test
    void findByReferenceNumberAndCardId_NonExisting_ShouldReturnEmpty() {
        StepVerifier.create(
                transactionPersistenceAdapter.findByReferenceNumberAndCardId("NO-EXISTE", "card-for-txn-test")
        )
                .verifyComplete();
    }
}
