-- ============================================================
-- iHair Projesi - PostgreSQL Tablo Oluşturma Script'i
-- Tüm tablolar ve kolonlar için açıklama (COMMENT ON) içerir.
-- Geliştirme ortamında sıfırdan oluşturmak için kullanılır.
-- ============================================================

-- Mevcut tabloları temizle (bağımlılık sırasına göre)
DROP TABLE IF EXISTS appointments   CASCADE;
DROP TABLE IF EXISTS campaigns      CASCADE;
DROP TABLE IF EXISTS hair_services  CASCADE;
DROP TABLE IF EXISTS employees      CASCADE;
DROP TABLE IF EXISTS customers      CASCADE;
DROP TABLE IF EXISTS salons         CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users          CASCADE;

-- ============================================================
-- USERS - Sistem Kullanıcıları (Auth)
-- ============================================================
CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(100)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'CUSTOMER',
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,

    CONSTRAINT chk_users_role
        CHECK (role IN ('ADMIN', 'SALON_OWNER', 'EMPLOYEE', 'CUSTOMER'))
);

COMMENT ON TABLE  users             IS 'Sisteme giriş yapabilen kullanıcı hesaplarını tutar';
COMMENT ON COLUMN users.id          IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN users.username    IS 'Giriş için kullanılan benzersiz kullanıcı adı';
COMMENT ON COLUMN users.password    IS 'BCrypt ile şifrelenmiş parola';
COMMENT ON COLUMN users.role        IS 'Kullanıcı rolü: ADMIN, SALON_OWNER, EMPLOYEE, CUSTOMER';
COMMENT ON COLUMN users.created_at  IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN users.updated_at  IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- REFRESH_TOKENS - JWT Yenileme Token'ları
-- ============================================================
CREATE TABLE refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    token       VARCHAR(512)    NOT NULL UNIQUE,
    user_id     BIGINT          NOT NULL UNIQUE,
    expires_at  TIMESTAMP       NOT NULL,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE  refresh_tokens             IS 'JWT refresh token kayıtlarını tutar; her kullanıcıya en fazla 1 token';
COMMENT ON COLUMN refresh_tokens.id          IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN refresh_tokens.token       IS 'JWT refresh token değeri (benzersiz)';
COMMENT ON COLUMN refresh_tokens.user_id     IS 'Token sahibi kullanıcının yabancı anahtarı (1-1 ilişki)';
COMMENT ON COLUMN refresh_tokens.expires_at  IS 'Token geçerlilik bitiş tarihi';

-- ============================================================
-- SALONS - Kuaför Salonları
-- ============================================================
CREATE TABLE salons (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    address     VARCHAR(500),
    phone       VARCHAR(20),
    email       VARCHAR(255),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

COMMENT ON TABLE  salons             IS 'Sisteme kayıtlı kuaför salonlarını tutar';
COMMENT ON COLUMN salons.id          IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN salons.name        IS 'Salonun ticari adı';
COMMENT ON COLUMN salons.address     IS 'Salonun açık adresi';
COMMENT ON COLUMN salons.phone       IS 'Salonun telefon numarası';
COMMENT ON COLUMN salons.email       IS 'Salonun e-posta adresi';
COMMENT ON COLUMN salons.active      IS 'Salonun aktiflik durumu; false ise soft delete (pasif)';
COMMENT ON COLUMN salons.created_at  IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN salons.updated_at  IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- EMPLOYEES - Çalışanlar
-- ============================================================
CREATE TABLE employees (
    id          BIGSERIAL       PRIMARY KEY,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(255),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    salon_id    BIGINT          NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,

    CONSTRAINT fk_employees_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id)
);

COMMENT ON TABLE  employees             IS 'Salonlarda çalışan personel bilgilerini tutar';
COMMENT ON COLUMN employees.id          IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN employees.first_name  IS 'Çalışanın adı';
COMMENT ON COLUMN employees.last_name   IS 'Çalışanın soyadı';
COMMENT ON COLUMN employees.phone       IS 'Çalışanın telefon numarası';
COMMENT ON COLUMN employees.email       IS 'Çalışanın e-posta adresi';
COMMENT ON COLUMN employees.active      IS 'Çalışanın aktiflik durumu; false ise soft delete (pasif)';
COMMENT ON COLUMN employees.salon_id    IS 'Çalışanın bağlı olduğu salonun yabancı anahtarı';
COMMENT ON COLUMN employees.created_at  IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN employees.updated_at  IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- CUSTOMERS - Müşteriler
-- ============================================================
CREATE TABLE customers (
    id          BIGSERIAL       PRIMARY KEY,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    phone       VARCHAR(20)     UNIQUE,
    email       VARCHAR(255)    UNIQUE,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    notes       TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

COMMENT ON TABLE  customers             IS 'Randevu alan müşteri bilgilerini tutar';
COMMENT ON COLUMN customers.id          IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN customers.first_name  IS 'Müşterinin adı';
COMMENT ON COLUMN customers.last_name   IS 'Müşterinin soyadı';
COMMENT ON COLUMN customers.phone       IS 'Müşterinin benzersiz telefon numarası';
COMMENT ON COLUMN customers.email       IS 'Müşterinin benzersiz e-posta adresi';
COMMENT ON COLUMN customers.active      IS 'Müşterinin aktiflik durumu; false ise soft delete (pasif)';
COMMENT ON COLUMN customers.notes       IS 'Müşteriye özel notlar (tercihler, alerji bilgisi vb.)';
COMMENT ON COLUMN customers.created_at  IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN customers.updated_at  IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- HAIR_SERVICES - Saç/Güzellik Hizmetleri
-- ============================================================
CREATE TABLE hair_services (
    id                 BIGSERIAL        PRIMARY KEY,
    name               VARCHAR(255)     NOT NULL,
    description        TEXT,
    price              NUMERIC(10, 2)   NOT NULL,
    duration_minutes   INTEGER          NOT NULL,
    active             BOOLEAN          NOT NULL DEFAULT TRUE,
    salon_id           BIGINT           NOT NULL,
    created_at         TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP,

    CONSTRAINT fk_hair_services_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id)
);

COMMENT ON TABLE  hair_services                   IS 'Salonların sunduğu hizmetleri (kesim, boya, vb.) tutar';
COMMENT ON COLUMN hair_services.id                IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN hair_services.name              IS 'Hizmetin adı (örn: Saç Kesimi, Boya)';
COMMENT ON COLUMN hair_services.description       IS 'Hizmetin detaylı açıklaması';
COMMENT ON COLUMN hair_services.price             IS 'Hizmetin standart fiyatı (TL cinsinden, 2 ondalık)';
COMMENT ON COLUMN hair_services.duration_minutes  IS 'Hizmetin tahmini süresi (dakika cinsinden)';
COMMENT ON COLUMN hair_services.active            IS 'Hizmetin aktiflik durumu; false ise soft delete (pasif)';
COMMENT ON COLUMN hair_services.salon_id          IS 'Hizmetin ait olduğu salonun yabancı anahtarı';
COMMENT ON COLUMN hair_services.created_at        IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN hair_services.updated_at        IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- CAMPAIGNS - Kampanya ve İndirim Kuponları
-- ============================================================
CREATE TABLE campaigns (
    id                   BIGSERIAL        PRIMARY KEY,
    name                 VARCHAR(255)     NOT NULL,
    description          TEXT,
    code                 VARCHAR(100)     NOT NULL UNIQUE,
    discount_type        VARCHAR(20)      NOT NULL,
    discount_value       NUMERIC(10, 2)   NOT NULL,
    max_usage_count      INTEGER,
    used_count           INTEGER          NOT NULL DEFAULT 0,
    is_customer_specific BOOLEAN          NOT NULL DEFAULT FALSE,
    customer_id          BIGINT,
    valid_from           TIMESTAMP,
    valid_to             TIMESTAMP,
    active               BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP,

    CONSTRAINT fk_campaigns_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT chk_campaigns_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SESSION')),
    CONSTRAINT chk_campaigns_discount_value
        CHECK (discount_value >= 0),
    CONSTRAINT chk_campaigns_used_count
        CHECK (used_count >= 0)
);

COMMENT ON TABLE  campaigns                        IS 'Kampanya ve indirim kuponu tanımlarını tutar';
COMMENT ON COLUMN campaigns.id                     IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN campaigns.name                   IS 'Kampanyanın görünen adı (örn: Kurucu Üye Kampanyası)';
COMMENT ON COLUMN campaigns.description            IS 'Kampanyanın detaylı açıklaması';
COMMENT ON COLUMN campaigns.code                   IS 'Benzersiz kampanya kodu; boş bırakılırsa sistem IH-XXXXXXXX formatında üretir';
COMMENT ON COLUMN campaigns.discount_type          IS 'İndirim tipi: PERCENTAGE (yüzde), FIXED_AMOUNT (sabit tutar), FREE_SESSION (ücretsiz seans)';
COMMENT ON COLUMN campaigns.discount_value         IS 'İndirim miktarı; PERCENTAGE için 0-100 arası, FIXED_AMOUNT için TL tutarı, FREE_SESSION için 0';
COMMENT ON COLUMN campaigns.max_usage_count        IS 'Maksimum kullanım sayısı; NULL ise sınırsız';
COMMENT ON COLUMN campaigns.used_count             IS 'Kampanyanın şimdiye kadar kullanılma sayısı';
COMMENT ON COLUMN campaigns.is_customer_specific   IS 'true ise yalnızca customer_id alanındaki müşteriye özeldir';
COMMENT ON COLUMN campaigns.customer_id            IS 'Müşteriye özel kampanyalarda hedef müşterinin yabancı anahtarı';
COMMENT ON COLUMN campaigns.valid_from             IS 'Kampanyanın geçerlilik başlangıç tarihi; NULL ise hemen geçerli';
COMMENT ON COLUMN campaigns.valid_to               IS 'Kampanyanın geçerlilik bitiş tarihi; NULL ise süresiz';
COMMENT ON COLUMN campaigns.active                 IS 'Kampanyanın aktiflik durumu; false ise soft delete (pasif)';
COMMENT ON COLUMN campaigns.created_at             IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN campaigns.updated_at             IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- APPOINTMENTS - Randevular
-- ============================================================
CREATE TABLE appointments (
    id                      BIGSERIAL        PRIMARY KEY,
    customer_id             BIGINT           NOT NULL,
    employee_id             BIGINT           NOT NULL,
    hair_service_id         BIGINT           NOT NULL,
    appointment_date_time   TIMESTAMP        NOT NULL,
    status                  VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    notes                   TEXT,
    campaign_id             BIGINT,
    final_price             NUMERIC(10, 2),
    created_at              TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,

    CONSTRAINT fk_appointments_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_appointments_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_appointments_hair_service
        FOREIGN KEY (hair_service_id) REFERENCES hair_services(id),
    CONSTRAINT fk_appointments_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT chk_appointments_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))
);

COMMENT ON TABLE  appointments                         IS 'Müşteri-çalışan randevu kayıtlarını tutar';
COMMENT ON COLUMN appointments.id                     IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN appointments.customer_id            IS 'Randevu alan müşterinin yabancı anahtarı';
COMMENT ON COLUMN appointments.employee_id            IS 'Randevuyu gerçekleştirecek çalışanın yabancı anahtarı';
COMMENT ON COLUMN appointments.hair_service_id        IS 'Alınacak hizmetin yabancı anahtarı';
COMMENT ON COLUMN appointments.appointment_date_time  IS 'Randevunun tarih ve saat bilgisi';
COMMENT ON COLUMN appointments.status                 IS 'Randevu durumu: PENDING (bekliyor), CONFIRMED (onaylandı), CANCELLED (iptal), COMPLETED (tamamlandı)';
COMMENT ON COLUMN appointments.notes                  IS 'Randevuya ait ek notlar veya müşteri istekleri';
COMMENT ON COLUMN appointments.campaign_id            IS 'Uygulanan kampanyanın yabancı anahtarı; NULL ise kampanyasız randevu';
COMMENT ON COLUMN appointments.final_price            IS 'Kampanya indirimi uygulandıktan sonraki nihai ücret; NULL ise kampanya kullanılmamış';
COMMENT ON COLUMN appointments.created_at             IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN appointments.updated_at             IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- İNDEKSLER - Sık sorgulanan kolonlar için performans
-- ============================================================
CREATE INDEX idx_employees_salon_id              ON employees(salon_id);
CREATE INDEX idx_employees_active                ON employees(active);
CREATE INDEX idx_hair_services_salon_id          ON hair_services(salon_id);
CREATE INDEX idx_hair_services_active            ON hair_services(active);
CREATE INDEX idx_customers_active                ON customers(active);
CREATE INDEX idx_salons_active                   ON salons(active);
CREATE INDEX idx_appointments_customer_id        ON appointments(customer_id);
CREATE INDEX idx_appointments_employee_id        ON appointments(employee_id);
CREATE INDEX idx_appointments_status             ON appointments(status);
CREATE INDEX idx_appointments_datetime           ON appointments(appointment_date_time);
CREATE INDEX idx_appointments_campaign_id        ON appointments(campaign_id);
CREATE INDEX idx_campaigns_code                  ON campaigns(code);
CREATE INDEX idx_campaigns_active                ON campaigns(active);
CREATE INDEX idx_campaigns_customer_id           ON campaigns(customer_id);

-- ============================================================
-- Başarı mesajı
-- ============================================================
DO $$
BEGIN
    RAISE NOTICE 'iHair tabloları ve açıklamaları başarıyla oluşturuldu.';
    RAISE NOTICE '  - users';
    RAISE NOTICE '  - refresh_tokens';
    RAISE NOTICE '  - salons';
    RAISE NOTICE '  - employees';
    RAISE NOTICE '  - customers';
    RAISE NOTICE '  - hair_services';
    RAISE NOTICE '  - campaigns';
    RAISE NOTICE '  - appointments';
END $$;
