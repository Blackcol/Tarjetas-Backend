package com.prueba_be.tarjetas.infrastructure.adapter.in.web;

import com.prueba_be.tarjetas.application.usecase.TransactionUseCase;
import com.prueba_be.tarjetas.domain.model.Transaction;
import com.prueba_be.tarjetas.domain.model.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

  @Mock
  private TransactionUseCase transactionUseCase;

  @InjectMocks
  private TransactionController transactionController;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToController(transactionController)
        .controllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void createTransaction_Success() {
    Transaction approvedTxn = Transaction.builder()
        .referenceNumber("112233")
        .status(TransactionStatus.APPROVED)
        .build();

    when(transactionUseCase.createTransaction("test-id", "112233", new BigDecimal("10.00"), "Test Address"))
        .thenReturn(Mono.just(approvedTxn));

    webTestClient.post().uri("/api/transaccion")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "identificador": "test-id",
              "numeroReferencia": "112233",
              "totalCompra": 10.00,
              "direccionCompra": "Test Address"
            }
            """)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.codigo").isEqualTo("00")
        .jsonPath("$.mensaje").isEqualTo("Compra exitosa")
        .jsonPath("$.estadoTransaccion").isEqualTo("Aprobada")
        .jsonPath("$.numeroReferencia").isEqualTo("112233");
  }

  @Test
  void createTransaction_InvalidData() {
    webTestClient.post().uri("/api/transaccion")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "identificador": "",
              "numeroReferencia": "12",
              "totalCompra": 0,
              "direccionCompra": ""
            }
            """)
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void annulTransaction_Success() {
    Transaction annulledTxn = Transaction.builder()
        .referenceNumber("123456")
        .build();

    when(transactionUseCase.annulTransaction("test-id", "123456", new BigDecimal("10.00")))
        .thenReturn(Mono.just(annulledTxn));

    webTestClient.post().uri("/api/transaccion/anular")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "identificador": "test-id",
              "numeroReferencia": "123456",
              "totalCompra": 10.00
            }
            """)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.codigo").isEqualTo("00")
        .jsonPath("$.mensaje").isEqualTo("Compra anulada")
        .jsonPath("$.numeroReferencia").isEqualTo("123456");
  }

  @Test
  void annulTransaction_InvalidData() {
    webTestClient.post().uri("/api/transaccion/anular")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "identificador": "",
              "numeroReferencia": "",
              "totalCompra": 0
            }
            """)
        .exchange()
        .expectStatus().isBadRequest();
  }
}
