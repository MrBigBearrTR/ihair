-- Satış ve gelir raporlama modülü. PostgreSQL üzerinde tekrar çalıştırılabilir.

CREATE TABLE IF NOT EXISTS sales (
    id                    BIGSERIAL PRIMARY KEY,
    salon_id              BIGINT NOT NULL REFERENCES salons(id),
    customer_id           BIGINT NOT NULL REFERENCES customers(id),
    source_appointment_id BIGINT UNIQUE REFERENCES appointments(id),
    created_by            BIGINT NOT NULL REFERENCES users(id),
    status                VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    subtotal              NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    completed_at          TIMESTAMP,
    cancelled_at          TIMESTAMP,
    notes                 TEXT,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    CONSTRAINT chk_sales_status
        CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_sales_amounts
        CHECK (subtotal >= 0 AND total_amount >= 0),
    CONSTRAINT chk_sales_lifecycle
        CHECK (
            (status = 'OPEN' AND completed_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL)
            OR (status = 'CANCELLED' AND completed_at IS NULL AND cancelled_at IS NOT NULL)
        )
);

CREATE TABLE IF NOT EXISTS sale_items (
    id                     BIGSERIAL PRIMARY KEY,
    sale_id                BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    hair_service_id        BIGINT NOT NULL REFERENCES hair_services(id),
    employee_id            BIGINT NOT NULL REFERENCES employees(id),
    quantity               INTEGER NOT NULL,
    position               INTEGER NOT NULL,
    unit_price             NUMERIC(12, 2) NOT NULL,
    list_price             NUMERIC(12, 2) NOT NULL,
    line_total             NUMERIC(12, 2) NOT NULL,
    service_name_snapshot  VARCHAR(255) NOT NULL,
    employee_name_snapshot VARCHAR(255) NOT NULL,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP,
    CONSTRAINT chk_sale_items_quantity CHECK (quantity BETWEEN 1 AND 100),
    CONSTRAINT chk_sale_items_position CHECK (position >= 0),
    CONSTRAINT chk_sale_items_amounts
        CHECK (unit_price >= 0 AND list_price >= 0 AND line_total >= 0),
    CONSTRAINT uq_sale_items_position UNIQUE (sale_id, position)
);

CREATE TABLE IF NOT EXISTS sale_payments (
    id         BIGSERIAL PRIMARY KEY,
    sale_id    BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    method     VARCHAR(30) NOT NULL,
    amount     NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT chk_sale_payments_method
        CHECK (method IN ('CASH', 'CARD', 'BANK_TRANSFER')),
    CONSTRAINT chk_sale_payments_amount CHECK (amount >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sales_source_appointment_idx
    ON sales(source_appointment_id)
    WHERE source_appointment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sales_salon_id ON sales(salon_id);
CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status);
CREATE INDEX IF NOT EXISTS idx_sales_completed_at ON sales(completed_at);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_employee_id ON sale_items(employee_id);
CREATE INDEX IF NOT EXISTS idx_sale_payments_sale_id ON sale_payments(sale_id);

COMMENT ON TABLE sales IS 'Açık, tamamlanmış ve iptal edilmiş finansal satış kayıtları';
COMMENT ON TABLE sale_items IS 'Satış anındaki hizmet, çalışan ve fiyat snapshot satırları';
COMMENT ON TABLE sale_payments IS 'Satış ödemeleri; veri modeli çoklu ödemeye hazırdır';
