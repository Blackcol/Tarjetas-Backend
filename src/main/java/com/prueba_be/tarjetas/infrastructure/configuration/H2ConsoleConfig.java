package com.prueba_be.tarjetas.infrastructure.configuration;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * Configuración para levantar la consola web de H2 en aplicaciones reactivas (WebFlux/Netty).
 *
 * En Spring WebFlux el servidor subyacente es Netty (no bloqueante), por lo que
 * la consola tradicional de H2 basada en Servlets NO funciona con
 * spring.h2.console.enabled=true. Esta clase levanta el servidor web embebido
 * de H2 de forma programática, exponiendo la consola en un puerto configurable.
 *
 * Se activa únicamente cuando la propiedad h2.console.enabled=true está presente.
 *
 * JDBC URL: jdbc:h2:mem:testdb;MODE=Oracle
 * Usuario: sa | Contraseña: (vacía)
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "h2.console.enabled", havingValue = "true")
public class H2ConsoleConfig {

    @Value("${h2.console.port:8082}")
    private String h2ConsolePort;

    private Server webServer;

    @EventListener(ContextRefreshedEvent.class)
    public void start() throws SQLException {
        this.webServer = Server.createWebServer("-webPort", h2ConsolePort, "-webAllowOthers").start();
        log.info("✅ Consola H2 iniciada en http://localhost:{}", this.webServer.getPort());
    }

    @EventListener(ContextClosedEvent.class)
    public void stop() {
        if (this.webServer != null) {
            this.webServer.stop();
            log.info("🛑 Consola H2 detenida");
        }
    }
}
