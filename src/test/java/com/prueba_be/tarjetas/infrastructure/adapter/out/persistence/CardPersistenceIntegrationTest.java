package com.prueba_be.tarjetas.infrastructure.adapter.out.persistence;

import com.prueba_be.tarjetas.domain.model.Card;
import com.prueba_be.tarjetas.domain.model.CardStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

/**
 * Test de integración contra H2 real.
 * Verifica que la persistencia funcione correctamente (INSERT, UPDATE, queries).
 *
 * Separado de los tests unitarios con Mockito, ya que no siempre
 * se permite ejecutar tests contra la base de datos en todos los entornos.
 */
@SpringBootTest
@ActiveProfiles("test")
class CardPersistenceIntegrationTest {

    @Autowired
    private CardPersistenceAdapter cardPersistenceAdapter;

    @Autowired
    private CardSpringDataRepository cardSpringDataRepository;

    @Test
    void save_NewCard_ShouldInsertInDatabase() {
        Card card = Card.builder()
                .id("hash-identifier-001")
                .maskedPan("123456******3456")
                .cardholder("Juan Perez")
                .nationalId("1010101010")
                .type("Crédito")
                .phone("3001234567")
                .status(CardStatus.CREATED)
                .validationNumber(42)
                .build();

        StepVerifier.create(cardPersistenceAdapter.save(card))
                .expectNextMatches(saved ->
                        saved.getId().equals("hash-identifier-001") &&
                        saved.getMaskedPan().equals("123456******3456") &&
                        saved.getCardholder().equals("Juan Perez") &&
                        saved.getStatus() == CardStatus.CREATED &&
                        saved.getValidationNumber() == 42
                )
                .verifyComplete();

        // Verificar que realmente está en la DB
        StepVerifier.create(cardSpringDataRepository.findByIdentifier("hash-identifier-001"))
                .expectNextMatches(entity ->
                        entity.getId() != null && // PK autoincremental generado
                        entity.getIdentifier().equals("hash-identifier-001") &&
                        entity.getMaskedPan().equals("123456******3456")
                )
                .verifyComplete();
    }

    @Test
    void save_ExistingCard_ShouldUpdateInDatabase() {
        Card card = Card.builder()
                .id("hash-identifier-002")
                .maskedPan("654321******6543")
                .cardholder("Maria Lopez")
                .nationalId("2020202020")
                .type("Débito")
                .phone("3109876543")
                .status(CardStatus.CREATED)
                .validationNumber(77)
                .build();

        // Guardar y luego actualizar el estado a ENROLLED
        StepVerifier.create(
                cardPersistenceAdapter.save(card)
                        .then(cardPersistenceAdapter.save(
                                Card.builder()
                                        .id("hash-identifier-002")
                                        .maskedPan("654321******6543")
                                        .cardholder("Maria Lopez")
                                        .nationalId("2020202020")
                                        .type("Débito")
                                        .phone("3109876543")
                                        .status(CardStatus.ENROLLED)
                                        .validationNumber(77)
                                        .build()
                        ))
        )
                .expectNextMatches(updated -> updated.getStatus() == CardStatus.ENROLLED)
                .verifyComplete();

        // Verificar que se actualizó (no se duplicó)
        StepVerifier.create(cardSpringDataRepository.findByIdentifier("hash-identifier-002"))
                .expectNextMatches(entity -> entity.getStatus().equals("Enrolada"))
                .verifyComplete();
    }

    @Test
    void findById_ExistingCard_ShouldReturnCard() {
        Card card = Card.builder()
                .id("hash-identifier-003")
                .maskedPan("111111******1111")
                .cardholder("Pedro Garcia")
                .nationalId("3030303030")
                .type("Crédito")
                .phone("3201112233")
                .status(CardStatus.CREATED)
                .validationNumber(15)
                .build();

        StepVerifier.create(
                cardPersistenceAdapter.save(card)
                        .then(cardPersistenceAdapter.findById("hash-identifier-003"))
        )
                .expectNextMatches(found ->
                        found.getId().equals("hash-identifier-003") &&
                        found.getCardholder().equals("Pedro Garcia")
                )
                .verifyComplete();
    }

    @Test
    void findById_NonExistingCard_ShouldReturnEmpty() {
        StepVerifier.create(cardPersistenceAdapter.findById("no-existe"))
                .verifyComplete();
    }
}
