package com.prueba_be.tarjetas.infrastructure.adapter.in.web;

import com.prueba_be.tarjetas.application.usecase.CardUseCase;
import com.prueba_be.tarjetas.domain.exception.DomainException;
import com.prueba_be.tarjetas.domain.model.Card;
import com.prueba_be.tarjetas.domain.model.CardStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

  @Mock
  private CardUseCase cardUseCase;

  @InjectMocks
  private CardController cardController;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToController(cardController)
        .controllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void createCard_Success() {
    Card savedCard = Card.builder()
        .id("test123")
        .maskedPan("123456******3456")
        .validationNumber(85)
        .build();

    when(cardUseCase.createCard(any())).thenReturn(Mono.just(savedCard));

    webTestClient.post().uri("/api/tarjeta")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "pan": "1234567890123456",
              "titular": "Juan Perez",
              "cedula": "1234567890",
              "tipo": "Crédito",
              "telefono": "3001234567"
            }
            """)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.codigo").isEqualTo("00")
        .jsonPath("$.numeroValidacion").isEqualTo(85)
        .jsonPath("$.pan").isEqualTo("123456******3456")
        .jsonPath("$.identificador").isEqualTo("test123");
  }

  @Test
  void createCard_InvalidPan() {
    webTestClient.post().uri("/api/tarjeta")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "pan": "invalid",
              "titular": "Juan",
              "cedula": "123",
              "tipo": "Crédito",
              "telefono": "123"
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.codigo").isEqualTo("03");
  }

  @Test
  void enrollCard_Success() {
    Card enrolledCard = Card.builder()
        .maskedPan("123456******3456")
        .build();

    when(cardUseCase.enrollCard("test123", 85)).thenReturn(Mono.just(enrolledCard));

    webTestClient.post().uri("/api/tarjeta/enrolar")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "identificador": "test123",
              "numeroValidacion": 85
            }
            """)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.codigo").isEqualTo("00")
        .jsonPath("$.pan").isEqualTo("123456******3456");
  }

  @Test
  void enrollCard_InvalidNumber() {
    when(cardUseCase.enrollCard(anyString(), anyInt()))
        .thenReturn(Mono.error(new DomainException("02", "Número de validación inválido")));

    webTestClient.post().uri("/api/tarjeta/enrolar")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "identificador": "test123",
              "numeroValidacion": 99
            }
            """)
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void consultCard_Success() {
    Card card = Card.builder()
        .maskedPan("123456******3456")
        .cardholder("Juan Perez")
        .nationalId("1234567890")
        .phone("3001234567")
        .status(CardStatus.ENROLLED)
        .build();

    when(cardUseCase.consultCard("test123")).thenReturn(Mono.just(card));

    webTestClient.get().uri("/api/tarjeta?identificador=test123")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.pan").isEqualTo("123456******3456")
        .jsonPath("$.estado").isEqualTo("Enrolada");
  }

  @Test
  void deleteCard_Success() {
    when(cardUseCase.deleteCard("test123")).thenReturn(Mono.empty());

    webTestClient.method(org.springframework.http.HttpMethod.DELETE).uri("/api/tarjeta")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "identificador": "test123"
            }
            """)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.codigo").isEqualTo("00");
  }
}
