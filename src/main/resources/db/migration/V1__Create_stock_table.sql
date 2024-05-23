CREATE TABLE store_stock
(
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id               TEXT        NOT NULL, -- normally this would be a FK to a store table, now just a string for simplicity
    product_id             TEXT        NOT NULL, -- normally this would be a FK to a product table, now just a string for simplicity
    stock                  BIGINT      NOT NULL DEFAULT 0,
    created_timestamp_utc  TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    modified_timestamp_utc TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    CONSTRAINT store_stock_ukey UNIQUE (store_id, product_id)
);

COMMENT ON TABLE store_stock IS 'Stock of a product in a store';
COMMENT ON COLUMN store_stock.id IS 'System generated primary key column';
COMMENT ON COLUMN store_stock.store_id IS 'The ID of the store in which the product is available';
COMMENT ON COLUMN store_stock.product_id IS 'The ID of the product in the store';
COMMENT ON COLUMN store_stock.stock IS 'Stock of the product in the store';
COMMENT ON COLUMN store_stock.created_timestamp_utc IS 'Metadata, timestamp when the stock record was created';
COMMENT ON COLUMN store_stock.modified_timestamp_utc IS 'Metadata, timestamp when the stock record was last updated';

