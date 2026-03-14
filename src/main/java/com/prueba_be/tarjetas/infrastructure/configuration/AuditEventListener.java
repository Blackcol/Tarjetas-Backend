package com.prueba_be.tarjetas.infrastructure.configuration;

import com.prueba_be.tarjetas.infrastructure.adapter.out.persistence.AuditEntity;
import com.prueba_be.tarjetas.infrastructure.adapter.out.persistence.CardEntity;
import com.prueba_be.tarjetas.infrastructure.adapter.out.persistence.TransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Listener de auditoría usando AfterSaveCallback (R2DBC reactivo).
 *
 * Usa DatabaseClient directamente (no el repository) para evitar
 * dependencia circular: AfterSaveCallback → Repository → R2dbcEntityTemplate → AfterSaveCallback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener implements AfterSaveCallback<Object> {

    private final DatabaseClient databaseClient;

    @Override
    public Publisher<Object> onAfterSave(Object entity, OutboundRow outboundRow, SqlIdentifier table) {
        // No auditar los propios registros de auditoría
        if (entity instanceof AuditEntity) {
            return Mono.just(entity);
        }

        String entityName = null;
        String entityId = null;
        String operation = null;

        if (entity instanceof CardEntity card) {
            entityName = "CARD";
            entityId = card.getIdentifier();
            operation = mapCardStatusToOperation(card.getStatus());
        } else if (entity instanceof TransactionEntity transaction) {
            entityName = "TRANSACTION";
            entityId = String.valueOf(transaction.getIdTransaction());
            // El label "Anulada" se guarda cuando hay anulación, "Aprobada" cuando es nueva transacción
            operation = "Anulada".equals(transaction.getStatus()) ? "ANULAR" : "CREAR";
        }

        if (entityName != null) {
            log.debug("Registrando auditoría: {} - {} - {}", entityName, entityId, operation);
            return databaseClient.sql("INSERT INTO AUDIT (entity_name, entity_id, operation, execution_date) VALUES (:name, :id, :op, :date)")
                    .bind("name", entityName)
                    .bind("id", entityId)
                    .bind("op", operation)
                    .bind("date", LocalDateTime.now())
                    .fetch()
                    .rowsUpdated()
                    .thenReturn(entity);
        }

        return Mono.just(entity);
    }

    private String mapCardStatusToOperation(String status) {
        return switch (status) {
            case "CREADA"    -> "CREAR";
            case "Enrolada"  -> "ENROLAR";
            case "INACTIVA"  -> "ELIMINAR";
            default          -> "DESCONOCIDO";
        };
    }
}
