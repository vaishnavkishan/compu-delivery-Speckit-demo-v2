CREATE TABLE bulk_order (
    id                           UUID PRIMARY KEY,
    client_id                    VARCHAR(64) NOT NULL REFERENCES enterprise_client(client_id),
    status                       VARCHAR(32) NOT NULL,
    gross_total                  NUMERIC(12,2) NOT NULL,
    applied_discount_percentage  NUMERIC(5,2) NOT NULL,
    net_total                    NUMERIC(12,2) NOT NULL,
    net_total_locked_at          TIMESTAMPTZ,
    version                      BIGINT NOT NULL DEFAULT 0,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at                 TIMESTAMPTZ
);

CREATE INDEX idx_bulk_order_client_created ON bulk_order(client_id, created_at DESC, id DESC);

CREATE TABLE line_item (
    id               UUID PRIMARY KEY,
    order_id         UUID NOT NULL REFERENCES bulk_order(id),
    sku              VARCHAR(64) NOT NULL REFERENCES hardware_catalog_item(sku),
    quantity         INTEGER NOT NULL,
    unit_list_price  NUMERIC(12,2) NOT NULL,
    line_subtotal    NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_line_item_order ON line_item(order_id);

CREATE TABLE lifecycle_transition (
    id           UUID PRIMARY KEY,
    order_id     UUID NOT NULL REFERENCES bulk_order(id),
    from_status  VARCHAR(32),
    to_status    VARCHAR(32) NOT NULL,
    actor_type   VARCHAR(16) NOT NULL,
    actor_id     VARCHAR(64) NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lifecycle_transition_order ON lifecycle_transition(order_id, occurred_at);

CREATE TABLE net_total_calculation (
    id                           UUID PRIMARY KEY,
    order_id                     UUID NOT NULL REFERENCES bulk_order(id),
    gross_total                  NUMERIC(12,2) NOT NULL,
    applied_discount_percentage  NUMERIC(5,2) NOT NULL,
    net_total                    NUMERIC(12,2) NOT NULL,
    trigger                      VARCHAR(32) NOT NULL,
    actor_id                     VARCHAR(64) NOT NULL,
    occurred_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_net_total_calculation_order ON net_total_calculation(order_id, occurred_at);

CREATE TABLE cancellation_record (
    id            UUID PRIMARY KEY,
    order_id      UUID NOT NULL UNIQUE REFERENCES bulk_order(id),
    cancelled_by  VARCHAR(64) NOT NULL,
    cancelled_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
