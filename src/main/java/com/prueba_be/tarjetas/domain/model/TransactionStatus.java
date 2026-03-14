package com.prueba_be.tarjetas.domain.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransactionStatus {
    APPROVED("Aprobada"),
    REJECTED("Rechazada"),
    ANNULLED("Anulada");

    // Valor que se almacena en BD y se muestra en respuestas API
    private final String label;

    /** Retorna el label en español que se almacena en BD y se muestra en respuestas API. */
    public String getLabel() {
        return label;
    }

    public static TransactionStatus fromLabel(String label) {
        for (TransactionStatus s : values()) {
            if (s.label.equalsIgnoreCase(label)) return s;
        }
        throw new IllegalArgumentException("Estado de transacción desconocido: " + label);
    }
}
