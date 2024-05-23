CREATE TABLE store_stock_reservation
(
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id               TEXT        NOT NULL,
    product_id             TEXT        NOT NULL,
    user_id                TEXT        NOT NULL,
    reserved_stock         BIGINT      NOT NULL DEFAULT 0,
    expires_at             TIMESTAMPTZ NOT NULL,
    created_timestamp_utc  TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    modified_timestamp_utc TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    FOREIGN KEY (store_id, product_id) REFERENCES store_stock (store_id, product_id) ON DELETE CASCADE,
    CONSTRAINT store_stock_reservation_ukey UNIQUE (store_id, product_id, user_id)

);

COMMENT ON TABLE store_stock_reservation IS 'Stock of a product in a store';
COMMENT ON COLUMN store_stock_reservation.id IS 'System generated primary key column';
COMMENT ON COLUMN store_stock_reservation.store_id IS 'The ID of the store in which the product is available';
COMMENT ON COLUMN store_stock_reservation.product_id IS 'The ID of the product in the store';
COMMENT ON COLUMN store_stock_reservation.user_id IS 'The ID of the user who reserved the stock';
COMMENT ON COLUMN store_stock_reservation.reserved_stock IS 'Stock of the product in the store';
COMMENT ON COLUMN store_stock_reservation.expires_at IS 'The timestamp when the reservation expires';
COMMENT ON COLUMN store_stock_reservation.created_timestamp_utc IS 'Metadata, timestamp when the stock record was created';
COMMENT ON COLUMN store_stock_reservation.modified_timestamp_utc IS 'Metadata, timestamp when the stock record was last updated';

