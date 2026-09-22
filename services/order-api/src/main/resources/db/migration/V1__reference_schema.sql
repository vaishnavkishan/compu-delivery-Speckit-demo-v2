CREATE TABLE enterprise_client (
    client_id    VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE operator (
    operator_id  VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE contract_discount_terms (
    id                   UUID PRIMARY KEY,
    client_id            VARCHAR(64) NOT NULL REFERENCES enterprise_client(client_id),
    discount_percentage  NUMERIC(5,2) NOT NULL,
    effective_from       TIMESTAMPTZ NOT NULL,
    effective_until      TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_contract_discount_terms_client ON contract_discount_terms(client_id);

CREATE TABLE hardware_catalog_item (
    sku        VARCHAR(64) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    list_price NUMERIC(12,2) NOT NULL
);
