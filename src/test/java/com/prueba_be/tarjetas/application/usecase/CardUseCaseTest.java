package com.prueba_be.tarjetas.application.usecase;

import com.prueba_be.tarjetas.domain.exception.DomainException;
import com.prueba_be.tarjetas.domain.model.CardStatus;
import com.prueba_be.tarjetas.domain.model.Card;
import com.prueba_be.tarjetas.domain.port.CardRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardUseCaseTest {

    @Mock
    private CardRepositoryPort cardRepositoryPort;

    @InjectMocks
    private CardUseCase cardUseCase;

    private Card requestCard;
    private Card createdCard;
    private Card inactiveCard;

    @BeforeEach
    void setUp() {
        requestCard = Card.builder()
                .maskedPan("1234567890123456")
                .cardholder("TEST USER")
                .nationalId("1234567890")
                .type("Crédito")
                .phone("3000000000")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(cardUseCase, "aesSecretKey", "1234567890123456");
        org.springframework.test.util.ReflectionTestUtils.setField(cardUseCase, "validationNumberMax", 100);
        org.springframework.test.util.ReflectionTestUtils.setField(cardUseCase, "identifierLength", 15);
        org.springframework.test.util.ReflectionTestUtils.setField(cardUseCase, "maskPrefixLength", 6);
        org.springframework.test.util.ReflectionTestUtils.setField(cardUseCase, "maskSuffixLength", 4);

        createdCard = Card.builder()
                .id("abcdefghijklmno")
                .maskedPan("123456******3456")
                .cardholder("TEST USER")
                .nationalId("1234567890")
                .type("Crédito")
                .phone("3000000000")
                .status(CardStatus.CREATED)
                .validationNumber(50)
                .build();

        inactiveCard = Card.builder()
                .id("inactive123")
                .maskedPan("999999******9999")
                .cardholder("INACTIVE USER")
                .nationalId("9876543210")
                .type("Débito")
                .phone("3100000000")
                .status(CardStatus.INACTIVE)
                .validationNumber(99)
                .build();
    }

    @Test
    void createCard_Success() {
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cardUseCase.createCard(requestCard))
                .expectNextMatches(t -> 
                    t.getStatus() == CardStatus.CREATED &&
                    t.getValidationNumber() >= 1 && t.getValidationNumber() <= 100 &&
                    t.getMaskedPan().contains("******")
                )
                .verifyComplete();
    }

    @Test
    void enrollCard_Success() {
        when(cardRepositoryPort.findById("abcdefghijklmno")).thenReturn(Mono.just(createdCard));
        when(cardRepositoryPort.save(any(Card.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cardUseCase.enrollCard("abcdefghijklmno", 50))
                .expectNextMatches(t -> t.getStatus() == CardStatus.ENROLLED)
                .verifyComplete();
    }

    @Test
    void enrollCard_InvalidNumber_ThrowsException() {
        when(cardRepositoryPort.findById("abcdefghijklmno")).thenReturn(Mono.just(createdCard));

        StepVerifier.create(cardUseCase.enrollCard("abcdefghijklmno", 99))
                .expectErrorMatches(throwable -> throwable instanceof DomainException &&
                        ((DomainException) throwable).getCode().equals("02"))
                .verify();
    }

    @Test
    @DisplayName("No se puede eliminar tarjeta inactiva - lanza DomainException 05")
    void cannotDeleteInactiveCard_ThrowsDomainException() {
        when(cardRepositoryPort.findById("inactive123")).thenReturn(Mono.just(inactiveCard));

        StepVerifier.create(cardUseCase.deleteCard("inactive123"))
                .expectErrorMatches(throwable -> throwable instanceof DomainException &&
                    "05".equals(((DomainException) throwable).getCode()) &&
                    throwable.getMessage().contains("No se puede eliminar tarjeta inactiva"))
                .verify();
    }

    @Test
    @DisplayName("Delete tarjeta activa exitoso")
    void deleteActiveCard_Success() {
        Card activeCard = Card.builder()
                .id("active456")
                .maskedPan("111111******1111")
                .cardholder("Active User")
                .nationalId("1111111111")
                .type("Crédito")
                .phone("3111111111")
                .status(CardStatus.ENROLLED)
                .validationNumber(42)
                .build();

        Card inactiveCard = Card.builder()
                .id("active456")
                .maskedPan("111111******1111")
                .cardholder("Active User")
                .nationalId("1111111111")
                .type("Crédito")
                .phone("3111111111")
                .status(CardStatus.INACTIVE)
                .validationNumber(42)
                .build();

        when(cardRepositoryPort.findById("active456")).thenReturn(Mono.just(activeCard));
        when(cardRepositoryPort.save(any(Card.class))).thenReturn(Mono.just(inactiveCard));

        StepVerifier.create(cardUseCase.deleteCard("active456"))
                .verifyComplete();
    }

    @Test
    void consultCard_Success() {
        when(cardRepositoryPort.findById("abcdefghijklmno")).thenReturn(Mono.just(createdCard));

        StepVerifier.create(cardUseCase.consultCard("abcdefghijklmno"))
                .expectNext(createdCard)
                .verifyComplete();
    }

    @Test
    void consultCard_NotFound_ThrowsException() {
        when(cardRepositoryPort.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(cardUseCase.consultCard("nonexistent"))
                .expectErrorMatches(throwable -> throwable instanceof DomainException &&
                        ((DomainException) throwable).getCode().equals("01"))
                .verify();
    }
}

