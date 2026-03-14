CREATE TABLE IF NOT EXISTS CARD (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    identifier VARCHAR2(255) NOT NULL UNIQUE,
    masked_pan VARCHAR2(255) NOT NULL,
    cardholder VARCHAR2(255) NOT NULL,
    national_id VARCHAR2(20) NOT NULL,
    type VARCHAR2(20) NOT NULL,
    phone VARCHAR2(15) NOT NULL,
    status VARCHAR2(20) NOT NULL,
    validation_number INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS TRANSACTION (
    id_transaction NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference_number VARCHAR2(20) NOT NULL,
    card_id VARCHAR2(255) NOT NULL,
    total_amount NUMBER(19,4) NOT NULL,
    purchase_address VARCHAR2(255) NOT NULL,
    status VARCHAR2(20) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_transaction_card FOREIGN KEY (card_id) REFERENCES CARD(identifier),
    CONSTRAINT uk_transaction UNIQUE (reference_number, card_id)
);

-- Tabla de Auditoría: registra todas las operaciones que modifican datos
CREATE TABLE IF NOT EXISTS AUDIT (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_name VARCHAR2(50) NOT NULL, -- Entidad afectada: CARD o TRANSACTION
    entity_id VARCHAR2(255) NOT NULL,
    operation VARCHAR2(50) NOT NULL, -- Operación: CREAR, ENROLAR, ELIMINAR, ANULAR
    execution_date TIMESTAMP NOT NULL
);
