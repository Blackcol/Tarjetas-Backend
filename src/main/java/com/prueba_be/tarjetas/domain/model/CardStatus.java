package com.prueba_be.tarjetas.domain.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CardStatus {
    CREATED("CREADA"),
    ENROLLED("Enrolada"),
    INACTIVE("INACTIVA");

    // Valor que se almacena en BD y se muestra en respuestas API
    private final String label;

    /** Retorna el label en español que se almacena en BD y se muestra en respuestas API. */
    public String getLabel() {
        return label;
    }

    public static CardStatus fromLabel(String label) {
        for (CardStatus s : values()) {
            if (s.label.equalsIgnoreCase(label)) return s;
        }
        throw new IllegalArgumentException("Estado de tarjeta desconocido: " + label);
    }
}
