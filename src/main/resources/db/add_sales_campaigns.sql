-- ARRIVED randevu durumu ve satış-geneli kampanya modeli.
-- PostgreSQL üzerinde tekrar çalıştırılabilir; ";;" statement ayırıcısı kullanılır.
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;;
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS chk_appointments_status;;
ALTER TABLE appointments ADD CONSTRAINT chk_appointments_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'ARRIVED', 'CANCELLED', 'COMPLETED'));;

ALTER TABLE sales ADD COLUMN IF NOT EXISTS campaign_id BIGINT;;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS campaign_code_snapshot VARCHAR(255);;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS campaign_name_snapshot VARCHAR(255);;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS campaign_discount_type_snapshot VARCHAR(20);;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS campaign_discount_value_snapshot NUMERIC(12, 2);;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS campaign_applied_at TIMESTAMP;;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_sales_campaign'
                     AND conrelid = 'sales'::regclass) THEN
        ALTER TABLE sales ADD CONSTRAINT fk_sales_campaign
            FOREIGN KEY (campaign_id) REFERENCES campaigns(id);
    END IF;
END $$;;

ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS discount_share NUMERIC(12, 2) NOT NULL DEFAULT 0;;
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS net_line_total NUMERIC(12, 2);;
UPDATE sale_items SET discount_share = 0 WHERE discount_share IS NULL;;
UPDATE sale_items SET net_line_total = line_total WHERE net_line_total IS NULL;;
ALTER TABLE sale_items ALTER COLUMN discount_share SET DEFAULT 0;;
ALTER TABLE sale_items ALTER COLUMN discount_share SET NOT NULL;;
ALTER TABLE sale_items ALTER COLUMN net_line_total SET NOT NULL;;
UPDATE sales SET discount_amount = 0 WHERE discount_amount IS NULL;;
UPDATE sales SET version = 0 WHERE version IS NULL;;
ALTER TABLE sales ALTER COLUMN discount_amount SET DEFAULT 0;;
ALTER TABLE sales ALTER COLUMN discount_amount SET NOT NULL;;
ALTER TABLE sales ALTER COLUMN version SET DEFAULT 0;;
ALTER TABLE sales ALTER COLUMN version SET NOT NULL;;

CREATE TABLE IF NOT EXISTS campaign_redemptions (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    campaign_id BIGINT NOT NULL REFERENCES campaigns(id),
    salon_id BIGINT NOT NULL REFERENCES salons(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    redeemed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_campaign_redemptions_sale UNIQUE (sale_id)
);;

INSERT INTO campaign_redemptions (
    sale_id, campaign_id, salon_id, customer_id, redeemed_at, created_at
)
SELECT s.id, a.campaign_id, s.salon_id, s.customer_id,
       COALESCE(s.completed_at, NOW()), COALESCE(s.completed_at, NOW())
FROM sales s
JOIN appointments a ON a.id = s.source_appointment_id
WHERE s.status = 'COMPLETED' AND a.campaign_id IS NOT NULL
ON CONFLICT (sale_id) DO NOTHING;;

UPDATE campaigns c SET used_count = (
    SELECT COUNT(*)::INTEGER FROM campaign_redemptions cr WHERE cr.campaign_id = c.id
);;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM campaigns GROUP BY UPPER(BTRIM(code)) HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Kampanya kodları büyük/küçük harf normalizasyonunda çakışıyor';
    END IF;
END $$;;
UPDATE campaigns SET code = UPPER(BTRIM(code));;
CREATE UNIQUE INDEX IF NOT EXISTS uq_campaigns_normalized_code
    ON campaigns (UPPER(BTRIM(code)));;

ALTER TABLE campaigns DROP CONSTRAINT IF EXISTS chk_campaigns_definition;;
ALTER TABLE campaigns ADD CONSTRAINT chk_campaigns_definition CHECK (
    (discount_type = 'PERCENTAGE' AND discount_value BETWEEN 0 AND 100)
    OR (discount_type = 'FIXED_AMOUNT' AND discount_value >= 0)
    OR (discount_type = 'FREE_SESSION' AND discount_value = 0)
) NOT VALID;;
ALTER TABLE campaigns DROP CONSTRAINT IF EXISTS chk_campaigns_max_usage;;
ALTER TABLE campaigns ADD CONSTRAINT chk_campaigns_max_usage
    CHECK (max_usage_count IS NULL OR max_usage_count > 0) NOT VALID;;
ALTER TABLE campaigns DROP CONSTRAINT IF EXISTS chk_campaigns_date_range;;
ALTER TABLE campaigns ADD CONSTRAINT chk_campaigns_date_range
    CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_to >= valid_from) NOT VALID;;
ALTER TABLE campaigns DROP CONSTRAINT IF EXISTS chk_campaigns_customer_scope;;
ALTER TABLE campaigns ADD CONSTRAINT chk_campaigns_customer_scope
    CHECK (NOT is_customer_specific OR customer_id IS NOT NULL) NOT VALID;;

ALTER TABLE sales DROP CONSTRAINT IF EXISTS chk_sales_campaign_amounts;;
ALTER TABLE sales ADD CONSTRAINT chk_sales_campaign_amounts CHECK (
    subtotal >= 0 AND discount_amount >= 0 AND total_amount >= 0
    AND discount_amount <= subtotal
    AND total_amount = subtotal - discount_amount
) NOT VALID;;
ALTER TABLE sales DROP CONSTRAINT IF EXISTS chk_sales_campaign_snapshot;;
ALTER TABLE sales ADD CONSTRAINT chk_sales_campaign_snapshot CHECK (
    (
        campaign_id IS NULL
        AND campaign_code_snapshot IS NULL
        AND campaign_name_snapshot IS NULL
        AND campaign_discount_type_snapshot IS NULL
        AND campaign_discount_value_snapshot IS NULL
        AND campaign_applied_at IS NULL
        AND discount_amount = 0
    ) OR (
        campaign_id IS NOT NULL
        AND campaign_code_snapshot IS NOT NULL
        AND campaign_name_snapshot IS NOT NULL
        AND campaign_discount_type_snapshot IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SESSION')
        AND campaign_discount_value_snapshot IS NOT NULL
        AND campaign_applied_at IS NOT NULL
    )
) NOT VALID;;
ALTER TABLE sale_items DROP CONSTRAINT IF EXISTS chk_sale_items_net_amounts;;
ALTER TABLE sale_items ADD CONSTRAINT chk_sale_items_net_amounts CHECK (
    discount_share >= 0 AND discount_share <= line_total
    AND net_line_total = line_total - discount_share
) NOT VALID;;

CREATE INDEX IF NOT EXISTS idx_sales_campaign_id ON sales(campaign_id);;
CREATE INDEX IF NOT EXISTS idx_campaign_redemptions_campaign_id
    ON campaign_redemptions(campaign_id);;
CREATE INDEX IF NOT EXISTS idx_campaign_redemptions_salon_id
    ON campaign_redemptions(salon_id);;
