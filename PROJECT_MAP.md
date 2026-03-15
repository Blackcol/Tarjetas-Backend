# 🗺️ Mapa del Proyecto - API Tarjetas y Transacciones

## 📋 Resumen General
API RESTful reactiva para administración de tarjetas de crédito/débito y transacciones de compra.
- **Framework:** Spring Boot 4.0.3 + WebFlux (Netty)
- **Arquitectura:** Hexagonal (Ports & Adapters) + DDD
- **Programación:** Reactiva (Mono/Flux con R2DBC)
- **Base de datos:** H2 en memoria (modo Oracle)
- **Tests:** JUnit 5 + Mockito + StepVerifier + JaCoCo (≥80%)

---

## 🏗️ Estructura del Proyecto

```
tarjetas/
├── build.gradle                          # Configuración Gradle, dependencias, JaCoCo, SonarQube
├── postman_collection.json               # Colección Postman con los 6 endpoints
├── settings.gradle                       # Nombre del proyecto Gradle
├── PROJECT_MAP.md                        # ← Este archivo (mapa del proyecto)
├── TODO.md                               # Seguimiento de tareas pendientes
│
├── src/main/resources/
│   ├── application.properties            # Configuración: R2DBC, actuadores, AES, H2 console
│   └── schema.sql                        # DDL: tablas CARD, TRANSACTION, AUDIT (sintaxis Oracle/H2)
│
├── src/main/java/com/prueba_be/tarjetas/
│   ├── CardsApplication.java             # Punto de entrada Spring Boot
│   │
│   ├── domain/                           # 🔵 CAPA DE DOMINIO (núcleo, sin dependencias externas)
│   │   ├── model/
│   │   │   ├── Card.java                 # Modelo de dominio: tarjeta (id, maskedPan, titular, etc.)
│   │   │   ├── CardStatus.java           # Enum: CREATED("CREADA"), ENROLLED("Enrolada"), INACTIVE("INACTIVA")
│   │   │   ├── Transaction.java          # Modelo de dominio: transacción (referencia, monto, dirección, etc.)
│   │   │   └── TransactionStatus.java    # Enum: APPROVED("Aprobada"), REJECTED("Rechazada"), ANNULLED("Anulada")
│   │   ├── exception/
│   │   │   └── DomainException.java      # Excepción personalizada con código ("00","01","02") y mensaje
│   │   └── port/
│   │       ├── CardRepositoryPort.java   # Puerto de salida: save(Card), findById(String)
│   │       └── TransactionRepositoryPort.java  # Puerto de salida: save(Transaction), findByRefAndCardId()
│   │
│   ├── application/                      # 🟢 CAPA DE APLICACIÓN (casos de uso / lógica de negocio)
│   │   └── usecase/
│   │       ├── CardUseCase.java          # Crear tarjeta, enrolar, consultar, eliminar (borrado lógico)
│   │       │                             #   - Genera hash AES (PAN + fecha) como identificador
│   │       │                             #   - Genera número de validación aleatorio (1-100)
│   │       │                             #   - Enmascara PAN: 123456******3456
│   │       └── TransactionUseCase.java   # Crear transacción (compra), anular transacción
│   │                                     #   - Valida tarjeta exista y esté enrolada
│   │                                     #   - Anulación solo si < 5 minutos desde creación
│   │
│   └── infrastructure/                   # 🟠 CAPA DE INFRAESTRUCTURA (adaptadores, config)
│       ├── adapter/
│       │   ├── in/web/                   # Adaptadores de ENTRADA (REST Controllers)
│       │   │   ├── CardController.java           # POST /api/tarjeta (crear)
│       │   │   │                                 # POST /api/tarjeta/enrolar
│       │   │   │                                 # GET  /api/tarjeta?identificador=X (consultar)
│       │   │   │                                 # DELETE /api/tarjeta (eliminar lógico)
│       │   │   │                                 # + DTOs: CreateCardRequest/Response, EnrollCardRequest/Response, etc.
│       │   │   ├── TransactionController.java    # POST /api/transaccion (crear compra)
│       │   │   │                                 # POST /api/transaccion/anular
│       │   │   │                                 # + DTOs: CreateTransactionRequest/Response, AnnulTransactionRequest/Response
│       │   │   └── GlobalExceptionHandler.java   # Manejo global: DomainException → 400, Validation → 400, General → 500
│       │   │
│       │   └── out/persistence/          # Adaptadores de SALIDA (persistencia R2DBC)
│       │       ├── CardEntity.java               # Entidad JPA/R2DBC → tabla CARD
│       │       ├── CardSpringDataRepository.java # R2dbcRepository: findByIdentifier()
│       │       ├── CardPersistenceAdapter.java   # Implementa CardRepositoryPort (save con INSERT/UPDATE, findById)
│       │       ├── TransactionEntity.java        # Entidad JPA/R2DBC → tabla TRANSACTION
│       │       ├── TransactionSpringDataRepository.java  # R2dbcRepository: findByReferenceNumberAndCardId()
│       │       ├── TransactionPersistenceAdapter.java    # Implementa TransactionRepositoryPort
│       │       ├── AuditEntity.java              # Entidad → tabla AUDIT (auditoría de operaciones)
│       │       └── AuditSpringDataRepository.java # R2dbcRepository para auditoría
│       │
│       └── configuration/                # Configuraciones de infraestructura
│           ├── AuditEventListener.java   # AfterSaveCallback: registra auditoría automática en BD
│           │                             #   - CARD: CREAR, ENROLAR, ELIMINAR
│           │                             #   - TRANSACTION: CREAR, ANULAR
│           ├── H2ConsoleConfig.java      # Consola H2 web en puerto 8082 (WebFlux no soporta servlet)
│           └── R2dbcAuditConfig.java     # Habilita @EnableR2dbcAuditing
│
├── src/test/resources/
│   └── application-test.properties       # Config de test: H2, consola desactivada, clave AES
│
└── src/test/java/com/prueba_be/tarjetas/
    ├── CardsApplicationTests.java                        # Test de carga de contexto Spring
    ├── application/usecase/
    │   ├── CardUseCaseTest.java                          # Tests unitarios: crear, enrolar, validación inválida
    │   └── TransactionUseCaseTest.java                   # Tests unitarios: crear txn, tarjeta no existe, anular, tiempo expirado
    └── infrastructure/adapter/out/persistence/
        ├── CardPersistenceIntegrationTest.java           # Tests integración: INSERT, UPDATE, findById, no encontrado
        └── TransactionPersistenceIntegrationTest.java    # Tests integración: INSERT, findByRef, no encontrado
```

---

## 🔄 Flujo de Datos (Ejemplo: Crear Tarjeta)

```
Cliente HTTP (Postman)
    │
    ▼
CardController.createCard()          ← Adaptador de ENTRADA (valida @Valid request)
    │  Mapea DTO → Card domain
    ▼
CardUseCase.createCard()             ← CASO DE USO (lógica de negocio)
    │  1. Genera identificador (hash AES: PAN + fecha)
    │  2. Genera número de validación (1-100)
    │  3. Enmascara PAN (123456******3456)
    │  4. Establece estado CREADA
    ▼
CardRepositoryPort.save()            ← PUERTO de salida (interfaz del dominio)
    │
    ▼
CardPersistenceAdapter.save()        ← Adaptador de SALIDA (implementación)
    │  Mapea Card → CardEntity
    ▼
CardSpringDataRepository.save()      ← Spring Data R2DBC (INSERT/UPDATE automático)
    │
    ▼
AuditEventListener.onAfterSave()     ← Callback automático post-guardado
    │  Registra en tabla AUDIT: entidad=CARD, operación=CREAR
    ▼
Respuesta reactiva (Mono<Card>) sube por la cadena hasta el Controller
    │  Mapea Card → CreateCardResponse DTO
    ▼
Cliente HTTP recibe JSON: { codigo: "00", mensaje: "Éxito", ... }
```

---

## 📡 Endpoints API

| Método | Ruta                        | Descripción                    | Códigos Respuesta |
|--------|-----------------------------|--------------------------------|-------------------|
| POST   | `/api/tarjeta`              | Crear tarjeta                  | 201, 400          |
| POST   | `/api/tarjeta/enrolar`      | Enrolar (activar) tarjeta      | 200, 400          |
| GET    | `/api/tarjeta?identificador=X` | Consultar tarjeta           | 200, 400          |
| DELETE | `/api/tarjeta`              | Eliminar tarjeta (lógico)      | 200, 400          |
| POST   | `/api/transaccion`          | Crear transacción (compra)     | 201, 400          |
| POST   | `/api/transaccion/anular`   | Anular transacción             | 200, 400          |
| GET    | `/actuator/health`          | Health check                   | 200               |
| GET    | `/actuator/info`            | Info de la aplicación          | 200               |

---

## 🗄️ Modelo de Base de Datos

```
┌─────────────────────────┐       ┌──────────────────────────────┐
│         CARD             │       │        TRANSACTION            │
├─────────────────────────┤       ├──────────────────────────────┤
│ id (PK, auto)           │       │ id_transaction (PK, auto)    │
│ identifier (UNIQUE)     │◄──────│ card_id (FK → CARD.identifier)│
│ masked_pan              │       │ reference_number              │
│ cardholder              │       │ total_amount                  │
│ national_id             │       │ purchase_address              │
│ type                    │       │ status                        │
│ phone                   │       │ creation_date                 │
│ status                  │       └──────────────────────────────┘
│ validation_number       │
└─────────────────────────┘
                                  ┌──────────────────────────────┐
                                  │           AUDIT               │
                                  ├──────────────────────────────┤
                                  │ id (PK, auto)                │
                                  │ entity_name (CARD/TRANSACTION)│
                                  │ entity_id                     │
                                  │ operation (CREAR/ENROLAR/etc) │
                                  │ execution_date                │
                                  └──────────────────────────────┘
```

---

## ✅ Checklist de Cumplimiento del Requerimiento

### Proyecto 1 – Backend

| # | Requerimiento | Estado | Archivo(s) |
|---|--------------|--------|------------|
| 1 | Crear tarjeta (PAN, titular, cédula, tipo, teléfono) | ✅ | CardController, CardUseCase |
| 2 | Retornar número validación (1-100) | ✅ | CardUseCase.createCard() |
| 3 | Estado inicial 'CREADA' | ✅ | CardUseCase.createCard() |
| 4 | Identificador hash (PAN + fecha) | ✅ | CardUseCase.generateIdentifier() |
| 5 | PAN enmascarado (6 primeros + 4 últimos) | ✅ | CardUseCase.maskPan() |
| 6 | Enrolar tarjeta (validar número validación) | ✅ | CardUseCase.enrollCard() |
| 7 | Consultar tarjeta por identificador | ✅ | CardUseCase.consultCard() |
| 8 | Eliminar tarjeta (borrado lógico → INACTIVA) | ✅ | CardUseCase.deleteCard() |
| 9 | Crear transacción (tarjeta enrolada) | ✅ | TransactionUseCase.createTransaction() |
| 10 | Anular transacción (< 5 minutos) | ✅ | TransactionUseCase.annulTransaction() |
| 11 | Pruebas unitarias (≥80% cobertura) | ✅ | CardUseCaseTest, TransactionUseCaseTest |
| 12 | Auditoría en BD | ✅ | AuditEventListener, tabla AUDIT |
| 13 | Spring Boot | ✅ | build.gradle |
| 14 | Validaciones javax/jakarta | ✅ | CardController, TransactionController |
| 15 | Actuadores (health check) | ✅ | application.properties |
| 16 | Códigos HTTP correctos (200, 201, 400, 500) | ✅ | Controllers, GlobalExceptionHandler |
| 17 | Excepciones personalizadas | ✅ | DomainException, GlobalExceptionHandler |
| 18 | Buenas prácticas (SOLID, Clean Architecture) | ✅ | Hexagonal + DDD |

### Aspectos del context.md

| # | Requerimiento | Estado | Notas |
|---|--------------|--------|-------|
| 1 | Java + Spring Boot | ✅ | |
| 2 | Programación reactiva (Flux/Mono) | ✅ | WebFlux + R2DBC |
| 3 | Arquitectura Hexagonal + DDD | ✅ | |
| 4 | Principios SOLID | ✅ | |
| 5 | JUnit + JaCoCo | ✅ | |
| 6 | SonarQube | ✅ | Configurado en build.gradle |
| 7 | Variables extraídas a config | ✅ | Clave AES via env var: `${TARJETAS_SECURITY_AES_SECRET:BancoSecreto1234}` |
| 8 | Tests unitarios + integración | ✅ | 100% success |
