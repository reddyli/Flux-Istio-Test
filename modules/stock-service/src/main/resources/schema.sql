CREATE TABLE IF NOT EXISTS inventory (
    sku       VARCHAR(64) PRIMARY KEY,
    quantity  INT         NOT NULL CHECK (quantity >= 0),
    version   BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS order_audit (
    event_id    UUID        PRIMARY KEY,
    order_id    UUID        NOT NULL,
    sku         VARCHAR(64) NOT NULL,
    quantity    INT         NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);