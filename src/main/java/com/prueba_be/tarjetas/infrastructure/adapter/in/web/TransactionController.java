package com.prueba_be.tarjetas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prueba_be.tarjetas.application.usecase.TransactionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Controlador REST para la gestión de transacciones de compra.
 * Adaptador de entrada que expone los endpoints HTTP y delega la lógica al caso de uso.
 */
@RestController
@RequestMapping("/api/transaccion")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateTransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionUseCase.createTransaction(request.getIdentifier(), request.getReferenceNumber(), request.getTotalAmount(), request.getPurchaseAddress())
                .map(t -> new CreateTransactionResponse("00", "Compra exitosa", t.getStatus().getLabel(), t.getReferenceNumber()));
    }

    @PostMapping("/anular")
    public Mono<AnnulTransactionResponse> annulTransaction(@Valid @RequestBody AnnulTransactionRequest request) {
        return transactionUseCase.annulTransaction(request.getIdentifier(), request.getReferenceNumber(), request.getTotalAmount())
                .map(t -> new AnnulTransactionResponse("00", "Compra anulada", t.getReferenceNumber()));
    }
}

@Data
class CreateTransactionRequest {
    @NotBlank(message="identificador no debe estar vacio")
    @JsonProperty("identificador")
    private String identifier;
    
    @NotBlank(message="numeroReferencia no debe estar vacio")
    @jakarta.validation.constraints.Size(min = 6, max = 6, message = "El número de referencia debe tener 6 dígitos")
    @Pattern(regexp = "\\d{6}", message = "El número de referencia debe contener solo 6 dígitos numéricos")
    @JsonProperty("numeroReferencia")
    private String referenceNumber;
    
    @NotNull(message="totalCompra no debe estar vacio")
    @DecimalMin("0.01")
    @JsonProperty("totalCompra")
    private BigDecimal totalAmount;
    
    @NotBlank(message="direccionCompra no debe estar vacio")
    @JsonProperty("direccionCompra")
    private String purchaseAddress;
}

record CreateTransactionResponse(
    @JsonProperty("codigo") String code, 
    @JsonProperty("mensaje") String message, 
    @JsonProperty("estadoTransaccion") String transactionStatus, 
    @JsonProperty("numeroReferencia") String referenceNumber
) {}

@Data
class AnnulTransactionRequest {
    @NotBlank(message="identificador no debe estar vacio")
    @JsonProperty("identificador")
    private String identifier;
    
    @NotBlank(message="numeroReferencia no debe estar vacio")
    @JsonProperty("numeroReferencia")
    private String referenceNumber;
    
    @NotNull(message="totalCompra no debe estar vacio")
    @DecimalMin("0.01")
    @JsonProperty("totalCompra")
    private BigDecimal totalAmount;
}

record AnnulTransactionResponse(
    @JsonProperty("codigo") String code, 
    @JsonProperty("mensaje") String message, 
    @JsonProperty("numeroReferencia") String referenceNumber
) {}
