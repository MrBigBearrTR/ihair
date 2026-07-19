-- Salon ve global uygulama logoları. PostgreSQL üzerinde tekrar çalıştırılabilir.
CREATE TABLE IF NOT EXISTS brand_assets (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    salon_id BIGINT,
    content_type VARCHAR(50) NOT NULL,
    data BYTEA NOT NULL,
    content_length BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'fk_brand_assets_salon'
                     AND conrelid = 'brand_assets'::regclass) THEN
        ALTER TABLE brand_assets ADD CONSTRAINT fk_brand_assets_salon
            FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_brand_assets_type'
                     AND conrelid = 'brand_assets'::regclass) THEN
        ALTER TABLE brand_assets ADD CONSTRAINT chk_brand_assets_type
            CHECK (type IN ('SALON_LOGO', 'GLOBAL_APP_LOGO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_brand_assets_relation'
                     AND conrelid = 'brand_assets'::regclass) THEN
        ALTER TABLE brand_assets ADD CONSTRAINT chk_brand_assets_relation CHECK (
            (type = 'SALON_LOGO' AND salon_id IS NOT NULL)
            OR (type = 'GLOBAL_APP_LOGO' AND salon_id IS NULL)
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_brand_assets_content_type'
                     AND conrelid = 'brand_assets'::regclass) THEN
        ALTER TABLE brand_assets ADD CONSTRAINT chk_brand_assets_content_type
            CHECK (content_type = 'image/png');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_brand_assets_content_length'
                     AND conrelid = 'brand_assets'::regclass) THEN
        ALTER TABLE brand_assets ADD CONSTRAINT chk_brand_assets_content_length CHECK (
            content_length = octet_length(data)
            AND content_length BETWEEN 1 AND 1048576
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_brand_assets_dimensions'
                     AND conrelid = 'brand_assets'::regclass) THEN
        ALTER TABLE brand_assets ADD CONSTRAINT chk_brand_assets_dimensions
            CHECK (width BETWEEN 1 AND 4096 AND height BETWEEN 1 AND 4096);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'chk_brand_assets_checksum'
                     AND conrelid = 'brand_assets'::regclass) THEN
        ALTER TABLE brand_assets ADD CONSTRAINT chk_brand_assets_checksum
            CHECK (checksum ~ '^[0-9a-f]{64}$');
    END IF;
END
$$;;

CREATE UNIQUE INDEX IF NOT EXISTS uq_brand_assets_salon_logo
    ON brand_assets(salon_id) WHERE type = 'SALON_LOGO';;
CREATE UNIQUE INDEX IF NOT EXISTS uq_brand_assets_global_logo
    ON brand_assets(type) WHERE type = 'GLOBAL_APP_LOGO';;
CREATE INDEX IF NOT EXISTS idx_brand_assets_salon_id ON brand_assets(salon_id);;
