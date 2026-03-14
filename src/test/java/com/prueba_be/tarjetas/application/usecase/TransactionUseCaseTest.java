package com.prueba_be.tarjetas.application.usecase;

import com.prueba_be.tarjetas.domain.exception.DomainException;
import com.prueba_be.tarjetas.domain.model.CardStatus;
import com.prueba_be.tarjetas.domain.model.TransactionStatus;
import com.prueba_be.tarjetas.domain.model.Card;
import com.prueba_be.tarjetas.domain.model.Transaction;
import com.prueba_be.tarjetas.domain.port.CardRepositoryPort;
import com.prueba_be.tarjetas.domain.port.TransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionUseCaseTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @Mock
    private CardRepositoryPort cardRepositoryPort;

    @InjectMocks
    private TransactionUseCase transactionUseCase;

    private Card enrolledCard;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(transactionUseCase, "annulTimeLimitMinutes", 5);
        enrolledCard = Card.builder()
                .id("abcdefghijklmno")
                .status(CardStatus.ENROLLED)
                .build();
    }

    @Test
    void createTransaction_Success() {
        when(cardRepositoryPort.findById("abcdefghijklmno")).thenReturn(Mono.just(enrolledCard));
        when(transactionRepositoryPort.findByReferenceNumberAndCardId("112233", "abcdefghijklmno")).thenReturn(Mono.empty());
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(transactionUseCase.createTransaction("abcdefghijklmno", "112233", BigDecimal.TEN, "Test Dir"))
                .expectNextMatches(t -> 
                    t.getStatus() == TransactionStatus.APPROVED &&
                    t.getReferenceNumber().equals("112233")
                )
                .verifyComplete();
    }

    @Test
    void createTransaction_CardNotExist_ThrowsException() {
        when(cardRepositoryPort.findById("noexistente")).thenReturn(Mono.empty());

        StepVerifier.create(transactionUseCase.createTransaction("noexistente", "112233", BigDecimal.TEN, "Test Dir"))
                .expectErrorMatches(throwable -> throwable instanceof DomainException &&
                        ((DomainException) throwable).getCode().equals("01"))
                .verify();
    }

    @Test
    void annulTransaction_Success() {
        Transaction t = Transaction.builder()
                .referenceNumber("123456")
                .cardId("abcdefghijklmno")
                .totalAmount(BigDecimal.TEN)
                .status(TransactionStatus.APPROVED)
                .creationDate(LocalDateTime.now())
                .build();

        when(transactionRepositoryPort.findByReferenceNumberAndCardId("123456", "abcdefghijklmno")).thenReturn(Mono.just(t));
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(transactionUseCase.annulTransaction("abcdefghijklmno", "123456", BigDecimal.TEN))
                .expectNextMatches(anulada -> anulada.getStatus() == TransactionStatus.ANNULLED)
                .verifyComplete();
    }

    @Test
    void annulTransaction_OutOfTime_ThrowsException() {
        Transaction t = Transaction.builder()
                .referenceNumber("123456")
                .cardId("abcdefghijklmno")
                .totalAmount(BigDecimal.TEN)
                .status(TransactionStatus.APPROVED)
                .creationDate(LocalDateTime.now().minusMinutes(6))
                .build();

        when(transactionRepositoryPort.findByReferenceNumberAndCardId("123456", "abcdefghijklmno")).thenReturn(Mono.just(t));

        StepVerifier.create(transactionUseCase.annulTransaction("abcdefghijklmno", "123456", BigDecimal.TEN))
                .expectErrorMatches(throwable -> throwable instanceof DomainException &&
                        throwable.getMessage().contains("Tiempo expirado"))
                .verify();
    }
}
