CREATE TABLE IF NOT EXISTS orders (
    id               UUID         PRIMARY KEY,
    sku              VARCHAR(64)  NOT NULL,
    quantity         INT          NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    rejection_reason TEXT,
    created_at       TIMESTAMPTZ  NOT NULL,
    event_id         UUID         UNIQUE,
    published_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_orders_unpublished
    ON orders (created_at)
    WHERE status = 'CONFIRMED' AND published_at IS NULL;
