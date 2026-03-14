package com.prueba_be.tarjetas.application.usecase;

import com.prueba_be.tarjetas.domain.exception.DomainException;
import com.prueba_be.tarjetas.domain.model.CardStatus;
import com.prueba_be.tarjetas.domain.model.Card;
import com.prueba_be.tarjetas.domain.port.CardRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Caso de uso para la gestión de tarjetas.
 * Contiene la lógica de negocio para crear, enrolar, consultar y eliminar
 * tarjetas.
 */
@Service
@RequiredArgsConstructor
public class CardUseCase {

    private final CardRepositoryPort cardRepositoryPort;
    private final Random random = new Random();

    @Value("${tarjetas.security.aes.secret}")
    private String aesSecretKey;

    @Value("${tarjetas.card.validation-number-max:100}")
    private int validationNumberMax;

    @Value("${tarjetas.card.identifier-length:15}")
    private int identifierLength;

    @Value("${tarjetas.card.mask-prefix-length:6}")
    private int maskPrefixLength;

    @Value("${tarjetas.card.mask-suffix-length:4}")
    private int maskSuffixLength;

    /**
     * Crea una nueva tarjeta en el sistema.
     * Genera un identificador único (hash AES del PAN + fecha), un número de
     * validación
     * aleatorio (1-100) y enmascara el PAN antes de persistir.
     * La tarjeta se almacena con estado CREADA.
     *
     * @param cardRequest datos de la tarjeta recibidos desde el controller
     *                    (maskedPan contiene el PAN crudo en este punto)
     * @return Mono con la tarjeta creada incluyendo identificador y PAN enmascarado
     */
    public Mono<Card> createCard(Card cardRequest) {
        // Se usa el PAN crudo (viene en maskedPan del request) para generar el hash
        String identifier = generateIdentifier(cardRequest.getMaskedPan());
        int validationNumber = random.nextInt(validationNumberMax) + 1;

        Card newCard = Card.builder()
                .id(identifier)
                .maskedPan(maskPan(cardRequest.getMaskedPan())) // Se enmascara el PAN crudo
                .cardholder(cardRequest.getCardholder())
                .nationalId(cardRequest.getNationalId())
                .type(cardRequest.getType())
                .phone(cardRequest.getPhone())
                .status(CardStatus.CREATED)
                .validationNumber(validationNumber)
                .build();

        return cardRepositoryPort.save(newCard);
    }

    /**
     * Enrola (activa) una tarjeta para poder realizar transacciones.
     * Solo se puede enrolar si está en estado CREADA y el número de validación
     * coincide.
     */
    public Mono<Card> enrollCard(String identifier, int validationNumber) {
        return cardRepositoryPort.findById(identifier)
                .switchIfEmpty(Mono.error(new DomainException("01", "Tarjeta no existe")))
                .flatMap(card -> {
                    if (card.getValidationNumber() != validationNumber) {
                        return Mono.error(new DomainException("02", "Número de validación inválido"));
                    }
                    if (card.getStatus() == CardStatus.ENROLLED) {
                        return Mono.just(card); // Ya está enrolada, se retorna sin cambios
                    }
                    if (card.getStatus() == CardStatus.INACTIVE) {
                        return Mono.error(new DomainException("02", "No se puede enrolar una tarjeta inactiva"));
                    }

                    Card enrolled = Card.builder()
                            .id(card.getId())
                            .maskedPan(card.getMaskedPan())
                            .cardholder(card.getCardholder())
                            .nationalId(card.getNationalId())
                            .type(card.getType())
                            .phone(card.getPhone())
                            .status(CardStatus.ENROLLED)
                            .validationNumber(card.getValidationNumber())
                            .build();

                    return cardRepositoryPort.save(enrolled);
                });
    }

    /**
     * Consulta los datos de una tarjeta por su identificador.
     */
    public Mono<Card> consultCard(String identifier) {
        return cardRepositoryPort.findById(identifier)
                .switchIfEmpty(Mono.error(new DomainException("01", "Tarjeta no encontrada")));
    }

    /**
     * Realiza un borrado lógico de la tarjeta, cambiando su estado a INACTIVA.
     * La tarjeta no se elimina físicamente de la base de datos.
     */
    public Mono<Void> deleteCard(String identifier) {
        return cardRepositoryPort.findById(identifier)
                .switchIfEmpty(Mono.error(new DomainException("01", "No se ha eliminado la tarjeta")))
                .flatMap(card -> {
                    if (card.getStatus() == CardStatus.INACTIVE) {
                        return Mono.error(new DomainException("05", "No se puede eliminar tarjeta inactiva"));
                    }

                    Card inactive = Card.builder()
                            .id(card.getId())
                            .maskedPan(card.getMaskedPan())
                            .cardholder(card.getCardholder())
                            .nationalId(card.getNationalId())
                            .type(card.getType())
                            .phone(card.getPhone())
                            .status(CardStatus.INACTIVE)
                            .validationNumber(card.getValidationNumber())
                            .build();
                    return cardRepositoryPort.save(inactive).then();
                });
    }

    private String generateIdentifier(String pan) {
        try {
            // Se cifra combinando el PAN (número de tarjeta) y la fecha actual usando AES
            // LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) guarda la fecha
            // local en formato 'yyyyMMdd'
            // Ejemplo: Para el 24 de Octubre de 2023, guardará exactamente la cadena
            // "20231024". No incluye hora.
            String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String input = pan + dateStr;

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // Se inyecta la llave simétrica desde la configuración (application.properties)
            SecretKeySpec secretKey = new SecretKeySpec(aesSecretKey.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : encrypted) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, identifierLength);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar identificador AES", e);
        }
    }

    /**
     * Enmascara el PAN dejando visibles solo los primeros 6 y últimos 4 dígitos.
     * Ejemplo: 1234567890123456 → 123456******3456
     */
    private String maskPan(String pan) {
        int minLength = maskPrefixLength + maskSuffixLength;
        if (pan == null || pan.length() < minLength)
            return pan;
        int length = pan.length();
        String start = pan.substring(0, maskPrefixLength);
        String end = pan.substring(length - maskSuffixLength);
        String masked = "*".repeat(length - minLength);
        return start + masked + end;
    }
}
