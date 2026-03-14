package com.prueba_be.tarjetas.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Configuración para habilitar la auditoría de R2DBC.
 * Permite que Spring Data R2DBC gestione callbacks de auditoría
 * (AfterSaveCallback) de forma reactiva.
 */
@Configuration
@EnableR2dbcAuditing
public class R2dbcAuditConfig {
}
