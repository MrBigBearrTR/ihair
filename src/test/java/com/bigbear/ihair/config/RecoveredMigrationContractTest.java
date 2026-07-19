package com.bigbear.ihair.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveredMigrationContractTest {
    @Test
    void salesCampaignMigrationContainsArrivedSnapshotsAndRedemptions() throws Exception {
        String sql = sql("db/add_sales_campaigns.sql");
        assertTrue(sql.contains("'ARRIVED'"));
        assertTrue(sql.contains("campaign_code_snapshot"));
        assertTrue(sql.contains("campaign_redemptions"));
        assertTrue(sql.contains("ON CONFLICT (sale_id) DO NOTHING"));
        assertTrue(sql.contains("net_line_total = line_total - discount_share"));
    }

    @Test
    void brandAndIntegrityMigrationsRemainIdempotent() throws Exception {
        String brand = sql("db/add_brand_assets.sql");
        String integrity = sql("db/enforce_salon_integrity.sql");
        assertTrue(brand.contains("CREATE TABLE IF NOT EXISTS brand_assets"));
        assertTrue(brand.contains("BYTEA NOT NULL"));
        assertTrue(brand.contains("CREATE UNIQUE INDEX IF NOT EXISTS"));
        assertTrue(integrity.contains("DROP TRIGGER IF EXISTS"));
        assertTrue(integrity.contains("appointments ALTER COLUMN salon_id SET NOT NULL"));
    }

    private String sql(String path) throws Exception {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
