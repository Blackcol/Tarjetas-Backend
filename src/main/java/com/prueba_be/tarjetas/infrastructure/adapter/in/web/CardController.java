package com.prueba_be.tarjetas.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prueba_be.tarjetas.application.usecase.CardUseCase;
import com.prueba_be.tarjetas.domain.model.Card;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controlador REST para la gestión de tarjetas.
 * Adaptador de entrada que expone los endpoints HTTP y delega la lógica al caso de uso.
 */
@RestController
@RequestMapping("/api/tarjeta")
@RequiredArgsConstructor
public class CardController {

    private final CardUseCase cardUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateCardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        Card domainRequest = Card.builder()
                .maskedPan(request.getPan()) // se usa el valor del request, se enmascara en caso de uso
                .cardholder(request.getCardholder())
                .nationalId(request.getNationalId())
                .type(request.getType())
                .phone(request.getPhone())
                .build();

        return cardUseCase.createCard(domainRequest)
                .map(t -> new CreateCardResponse("00", "Éxito", t.getValidationNumber(), t.getMaskedPan(), t.getId()));
    }

    @PostMapping("/enrolar")
    public Mono<EnrollCardResponse> enrollCard(@Valid @RequestBody EnrollCardRequest request) {
        return cardUseCase.enrollCard(request.getIdentifier(), request.getValidationNumber())
                .map(t -> new EnrollCardResponse("00", "Éxito", t.getMaskedPan()));
    }

    @GetMapping
    public Mono<ConsultCardResponse> consultCard(@RequestParam("identificador") String identifier) {
        return cardUseCase.consultCard(identifier)
                .map(t -> new ConsultCardResponse(t.getMaskedPan(), t.getCardholder(), t.getNationalId(), t.getPhone(),
                        t.getStatus().getLabel()));
    }

    @DeleteMapping
    public Mono<DeleteCardResponse> deleteCard(@Valid @RequestBody DeleteCardRequest request) {
        return cardUseCase.deleteCard(request.getIdentifier())
                .then(Mono.just(new DeleteCardResponse("00", "Se ha eliminado la tarjeta")));
    }
}

@Data
class CreateCardRequest {
    @NotBlank(message="pan no debe estar vacio")
    @Size(min = 16, max = 19)
    @Pattern(regexp = "\\d+", message = "PAN debe contener solo números")
    @JsonProperty("pan")
    private String pan;

    @NotBlank(message="titular no debe estar vacio")
    @JsonProperty("titular")
    private String cardholder;

    @NotBlank(message="cedula no debe estar vacio")
    @Size(min = 10, max = 15)
    @JsonProperty("cedula")
    private String nationalId;

    @NotBlank(message="tipo no debe estar vacio")
    @Pattern(regexp = "^(Crédito|Débito)$", message = "Tipo debe ser Crédito o Débito")
    @JsonProperty("tipo")
    private String type;

    @NotBlank(message="telefono no debe estar vacio")
    @Size(min = 10, max = 10)
    @Pattern(regexp = "\\d+", message = "Telefono debe contener solo números")
    @JsonProperty("telefono")
    private String phone;
}

record CreateCardResponse(
        @JsonProperty("codigo") String code,
        @JsonProperty("mensaje") String message,
        @JsonProperty("numeroValidacion") Integer validationNumber,
        @JsonProperty("pan") String maskedPan,
        @JsonProperty("identificador") String identifier) {
}

@Data
class EnrollCardRequest {
    @NotBlank(message="identificador no debe estar vacio")
    @JsonProperty("identificador")
    private String identifier;

    @JsonProperty("numeroValidacion")
    private int validationNumber;
}

record EnrollCardResponse(
        @JsonProperty("codigo") String code,
        @JsonProperty("mensaje") String message,
        @JsonProperty("pan") String maskedPan) {
}

record ConsultCardResponse(
        @JsonProperty("pan") String maskedPan,
        @JsonProperty("titular") String cardholder,
        @JsonProperty("cedula") String nationalId,
        @JsonProperty("telefono") String phone,
        @JsonProperty("estado") String status) {
}

@Data
class DeleteCardRequest {
    @NotBlank(message="identificador no debe estar vacio")
    @JsonProperty("identificador")
    private String identifier;
}

record DeleteCardResponse(
        @JsonProperty("codigo") String code,
        @JsonProperty("mensaje") String message) {
}
