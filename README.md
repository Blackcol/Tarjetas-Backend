# Tarjetas - API REST de Gestión de Tarjetas y Transacciones

API reactiva para la gestión de tarjetas de crédito/débito y transacciones de compra, construida con **Spring Boot 4 + WebFlux** siguiendo **Arquitectura Hexagonal (Ports & Adapters)**.

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Framework | Spring Boot 4.0.3 + WebFlux (reactivo) |
| Lenguaje | Java 21 |
| Base de datos | H2 en memoria (desarrollo) / Oracle (producción) |
| Acceso a datos | Spring Data R2DBC (no bloqueante) |
| Build | Gradle |
| Cobertura | JaCoCo (mínimo 80%) |
| Observabilidad | Spring Actuator + Micrometer Prometheus |

## Inicio Rápido

### Prerequisitos

- **Java 21** (OpenJDK o similar)
- **Git**

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd proyecto/backend/tarjetas
```

### 2. Compilar y ejecutar tests

```bash
./gradlew build
```

### 3. Levantar la aplicación

```bash
./gradlew bootRun
```

La API estará disponible en `http://localhost:8080`.

> No se requiere instalar ni configurar base de datos. La aplicación usa **H2 en memoria** por defecto con el esquema auto-generado desde `schema.sql`.

---

## Health Checks y Observabilidad

Spring Actuator expone endpoints de monitoreo en `/actuator`:

### Verificar que la aplicación está viva

```bash
curl http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "r2dbc": { "status": "UP" }
  }
}
```

### Endpoints de Actuator disponibles

| Endpoint | Descripción |
|---|---|
| `GET /actuator/health` | Estado general de la aplicación y sus componentes |
| `GET /actuator/info` | Información de la aplicación |
| `GET /actuator/metrics` | Listado de métricas disponibles |
| `GET /actuator/metrics/{nombre}` | Detalle de una métrica específica (ej: `jvm.memory.used`) |
| `GET /actuator/prometheus` | Métricas en formato Prometheus (para Grafana) |
| `GET /actuator/mappings` | Todos los endpoints REST registrados |
| `GET /actuator/loggers` | Niveles de log configurados |
| `POST /actuator/loggers/{logger}` | Cambiar nivel de log en caliente |

### Cambiar nivel de log sin reiniciar

```bash
curl -X POST http://localhost:8080/actuator/loggers/com.prueba_be.tarjetas \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## Endpoints de la API

Base URL: `http://localhost:8080`

### Tarjetas

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/tarjeta` | Crear tarjeta |
| `POST` | `/api/tarjeta/enrolar` | Enrolar (activar) tarjeta |
| `GET` | `/api/tarjeta?identificador={id}` | Consultar tarjeta |
| `DELETE` | `/api/tarjeta` | Eliminar tarjeta (borrado lógico) |

### Transacciones

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/transaccion` | Crear transacción de compra |
| `POST` | `/api/transaccion/anular` | Anular transacción |

### Ejemplos de uso

**Crear tarjeta:**

```bash
curl -X POST http://localhost:8080/api/tarjeta \
  -H "Content-Type: application/json" \
  -d '{
    "pan": "4000123456789010",
    "titular": "Juan Pérez",
    "cedula": "1234567890",
    "tipo": "Crédito",
    "telefono": "3001234567"
  }'
```

```json
{
  "codigo": "00",
  "mensaje": "Éxito",
  "numeroValidacion": 42,
  "pan": "400012******9010",
  "identificador": "a1b2c3d4e5f6g7h"
}
```

**Enrolar tarjeta:**

```bash
curl -X POST http://localhost:8080/api/tarjeta/enrolar \
  -H "Content-Type: application/json" \
  -d '{
    "identificador": "a1b2c3d4e5f6g7h",
    "numeroValidacion": 42
  }'
```

**Crear transacción:**

```bash
curl -X POST http://localhost:8080/api/transaccion \
  -H "Content-Type: application/json" \
  -d '{
    "identificador": "a1b2c3d4e5f6g7h",
    "numeroReferencia": "112233",
    "totalCompra": 150000.00,
    "direccionCompra": "Calle 123, Bogotá"
  }'
```

**Anular transacción** (debe hacerse dentro del tiempo límite configurado):

```bash
curl -X POST http://localhost:8080/api/transaccion/anular \
  -H "Content-Type: application/json" \
  -d '{
    "identificador": "a1b2c3d4e5f6g7h",
    "numeroReferencia": "112233",
    "totalCompra": 150000.00
  }'
```

---

## Variables de Configuración

Todas las propiedades tienen valores por defecto funcionales. Se pueden sobrescribir mediante `application.properties`, perfiles de Spring (`application-{perfil}.properties`) o **variables de entorno** sin necesidad de redesplegar.

### Seguridad

| Propiedad | Variable de entorno | Default | Descripción |
|---|---|---|---|
| `tarjetas.security.aes.secret` | `TARJETAS_SECURITY_AES_SECRET` | `BancoSecreto1234` | Llave simétrica AES de 16 caracteres para generar identificadores de tarjeta |

### CORS

| Propiedad | Variable de entorno | Default | Descripción |
|---|---|---|---|
| `tarjetas.cors.allowed-origins` | `TARJETAS_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:5174` | Orígenes permitidos, separados por coma |

### Reglas de negocio - Tarjetas

| Propiedad | Default | Descripción |
|---|---|---|
| `tarjetas.card.validation-number-max` | `100` | Valor máximo del número de validación aleatorio (rango: 1 a N) |
| `tarjetas.card.identifier-length` | `15` | Longitud del identificador hash AES generado |
| `tarjetas.card.mask-prefix-length` | `6` | Cantidad de dígitos visibles al inicio del PAN enmascarado |
| `tarjetas.card.mask-suffix-length` | `4` | Cantidad de dígitos visibles al final del PAN enmascarado |

### Reglas de negocio - Transacciones

| Propiedad | Default | Descripción |
|---|---|---|
| `tarjetas.transaction.annul-time-limit-minutes` | `5` | Tiempo máximo (en minutos) para anular una transacción después de crearla |

### Infraestructura

| Propiedad | Default | Descripción |
|---|---|---|
| `h2.console.enabled` | `true` | Habilita la consola web de H2 para inspección de datos |
| `h2.console.port` | `8082` | Puerto de la consola web de H2 |

### Ejemplo: sobrescribir con variables de entorno

```bash
# Linux/Mac
export TARJETAS_CORS_ALLOWED_ORIGINS=https://mi-frontend.com
export TARJETAS_SECURITY_AES_SECRET=MiLlaveSegura16!
./gradlew bootRun

# Windows PowerShell
$env:TARJETAS_CORS_ALLOWED_ORIGINS="https://mi-frontend.com"
$env:TARJETAS_SECURITY_AES_SECRET="MiLlaveSegura16!"
./gradlew bootRun
```

### Ejemplo: sobrescribir con argumentos de línea de comandos

```bash
./gradlew bootRun --args='--tarjetas.transaction.annul-time-limit-minutes=10 --tarjetas.card.validation-number-max=999'
```

---

## Consola H2 (solo desarrollo)

Cuando `h2.console.enabled=true`, la consola web de H2 está disponible para inspeccionar datos en tiempo real:

- **URL:** `http://localhost:8082`
- **JDBC URL:** `jdbc:h2:mem:testdb;MODE=Oracle`
- **Usuario:** `sa`
- **Contraseña:** *(vacía)*

---

## Arquitectura

```
src/main/java/com/prueba_be/tarjetas/
├── domain/                          # Capa de dominio (sin dependencias externas)
│   ├── model/                       # Entidades: Card, Transaction, enums de estado
│   ├── exception/                   # DomainException con códigos de error
│   └── port/                        # Puertos (interfaces): CardRepositoryPort, TransactionRepositoryPort
├── application/
│   └── usecase/                     # Casos de uso: CardUseCase, TransactionUseCase
└── infrastructure/
    ├── adapter/
    │   ├── in/web/                  # Controllers REST + DTOs + GlobalExceptionHandler
    │   └── out/persistence/         # Adaptadores R2DBC (entities, repositories, mappers)
    └── configuration/               # CorsConfig, H2ConsoleConfig, AuditConfig
```

### Flujo de una petición

```
HTTP Request → Controller → UseCase → Port (interfaz) → PersistenceAdapter → R2DBC → BD
```

Las capas internas (domain, application) no conocen las externas. Los puertos definen contratos que los adaptadores implementan, permitiendo cambiar la base de datos o el framework web sin modificar la lógica de negocio.

---

## Códigos de respuesta

| Código | Significado |
|---|---|
| `00` | Operación exitosa |
| `01` | Recurso no encontrado (tarjeta/transacción no existe) |
| `02` | Validación de negocio fallida (número inválido, tarjeta no enrolada, tiempo expirado, monto incorrecto) |
| `03` | Error de validación de campos (formato, campos requeridos) |
| `04` | Error de base de datos |
| `05` | Operación no permitida sobre el estado actual (ej: eliminar tarjeta inactiva) |
| `99` | Error interno del servidor |

---

## Tests

```bash
# Ejecutar todos los tests
./gradlew test

# Ver reporte de cobertura (se genera automáticamente)
# Abrir: build/reports/jacoco/test/html/index.html

# Ver reporte de tests
# Abrir: build/reports/tests/test/index.html
```

El proyecto mantiene un mínimo de **80% de cobertura** verificado por JaCoCo.

---

## Colección Postman

El archivo `postman_collection.json` incluido en la raíz del proyecto contiene todos los endpoints listos para importar en Postman.
