-- ============================================================
-- iHair Projesi - PostgreSQL Tablo Oluşturma Script'i
-- Tüm tablolar ve kolonlar için açıklama (COMMENT ON) içerir.
-- Geliştirme ortamında sıfırdan oluşturmak için kullanılır.
-- ============================================================

-- Mevcut tabloları temizle (bağımlılık sırasına göre)
DROP TABLE IF EXISTS sale_payments  CASCADE;
DROP TABLE IF EXISTS sale_items     CASCADE;
DROP TABLE IF EXISTS sales          CASCADE;
DROP TABLE IF EXISTS appointments   CASCADE;
DROP TABLE IF EXISTS campaigns      CASCADE;
DROP TABLE IF EXISTS hair_services  CASCADE;
DROP TABLE IF EXISTS employees      CASCADE;
DROP TABLE IF EXISTS salon_settings CASCADE;
DROP TABLE IF EXISTS customers      CASCADE;
DROP TABLE IF EXISTS salons         CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS user_salon_access CASCADE;
DROP TABLE IF EXISTS users          CASCADE;

-- ============================================================
-- USERS - Sistem Kullanıcıları (Auth)
-- ============================================================
CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(100)    NOT NULL UNIQUE,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'CUSTOMER',
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    salon_id    BIGINT,
    employee_id BIGINT          UNIQUE,
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
COMMENT ON COLUMN users.salon_id    IS 'SALON_OWNER ve EMPLOYEE kullanıcısının güvenli salon kapsamı';
COMMENT ON COLUMN users.employee_id IS 'EMPLOYEE rolündeki kullanıcının çalışan kaydı';
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
    salon_id    BIGINT,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,

    CONSTRAINT fk_customers_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id)
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
    duration_minutes   INTEGER          NOT NULL DEFAULT 30,
    active             BOOLEAN          NOT NULL DEFAULT TRUE,
    salon_id           BIGINT           NOT NULL,
    created_at         TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP,

    CONSTRAINT fk_hair_services_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id),
    CONSTRAINT chk_hair_services_duration
        CHECK (duration_minutes > 0)
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
    salon_id             BIGINT,
    valid_from           TIMESTAMP,
    valid_to             TIMESTAMP,
    active               BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP,

    CONSTRAINT fk_campaigns_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_campaigns_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id),
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
COMMENT ON COLUMN campaigns.salon_id               IS 'Kampanyanın ait olduğu salon; eski kapsam dışı kayıtlar için NULL olabilir';
COMMENT ON COLUMN campaigns.valid_from             IS 'Kampanyanın geçerlilik başlangıç tarihi; NULL ise hemen geçerli';
COMMENT ON COLUMN campaigns.valid_to               IS 'Kampanyanın geçerlilik bitiş tarihi; NULL ise süresiz';
COMMENT ON COLUMN campaigns.active                 IS 'Kampanyanın aktiflik durumu; false ise soft delete (pasif)';
COMMENT ON COLUMN campaigns.created_at             IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN campaigns.updated_at             IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- SALON_SETTINGS - Salon Sabit Bilgileri (Key-Value)
-- ============================================================
CREATE TABLE salon_settings (
    id             BIGSERIAL        PRIMARY KEY,
    salon_id       BIGINT           NOT NULL,
    setting_key    VARCHAR(100)     NOT NULL,
    setting_type   VARCHAR(20)      NOT NULL DEFAULT 'TEXT',
    setting_value  TEXT             NOT NULL,
    description    VARCHAR(255),
    created_at     TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP,

    CONSTRAINT fk_salon_settings_salon
        FOREIGN KEY (salon_id) REFERENCES salons(id) ON DELETE CASCADE,
    CONSTRAINT uq_salon_settings_salon_key
        UNIQUE (salon_id, setting_key),
    CONSTRAINT chk_salon_settings_type
        CHECK (setting_type IN ('TEXT', 'IMAGE_BASE64', 'URL', 'JSON'))
);

COMMENT ON TABLE  salon_settings               IS 'Salon bazlı sabit bilgileri (logo, adres, sosyal medya vb.) key-value yapısında tutar';
COMMENT ON COLUMN salon_settings.id            IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN salon_settings.salon_id      IS 'Ayarın ait olduğu salonun yabancı anahtarı';
COMMENT ON COLUMN salon_settings.setting_key   IS 'Ayar anahtarı (büyük harf normalize edilir; örn: LOGO, ADDRESS, WORKING_HOURS)';
COMMENT ON COLUMN salon_settings.setting_type  IS 'Değerin tipi: TEXT (düz metin), IMAGE_BASE64 (base64 görsel), URL (link), JSON (yapılandırılmış veri)';
COMMENT ON COLUMN salon_settings.setting_value IS 'Ayar değeri; setting_type''a göre yorumlanır';
COMMENT ON COLUMN salon_settings.description   IS 'Ayarın insan tarafından okunabilir açıklaması (opsiyonel)';
COMMENT ON COLUMN salon_settings.created_at    IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN salon_settings.updated_at    IS 'Kaydın son güncellenme tarihi (otomatik)';

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
-- SALES - Satışlar, satırlar ve ödemeler
-- ============================================================
CREATE TABLE sales (
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
    CONSTRAINT chk_sales_status CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_sales_amounts CHECK (subtotal >= 0 AND total_amount >= 0),
    CONSTRAINT chk_sales_lifecycle CHECK (
        (status = 'OPEN' AND completed_at IS NULL AND cancelled_at IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND completed_at IS NULL AND cancelled_at IS NOT NULL)
    )
);

CREATE TABLE sale_items (
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

CREATE TABLE sale_payments (
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

COMMENT ON TABLE sales IS 'Açık, tamamlanmış ve iptal edilmiş finansal satış kayıtları';
COMMENT ON TABLE sale_items IS 'Satış anındaki hizmet, çalışan ve fiyat snapshot satırları';
COMMENT ON TABLE sale_payments IS 'Satış ödemeleri; veri modeli çoklu ödemeye hazırdır';

-- ============================================================
-- İNDEKSLER - Sık sorgulanan kolonlar için performans
-- ============================================================
CREATE INDEX idx_employees_salon_id              ON employees(salon_id);
CREATE INDEX idx_employees_active                ON employees(active);
CREATE INDEX idx_hair_services_salon_id          ON hair_services(salon_id);
CREATE INDEX idx_hair_services_active            ON hair_services(active);
CREATE INDEX idx_customers_active                ON customers(active);
CREATE INDEX idx_customers_salon_id              ON customers(salon_id);
CREATE INDEX idx_salons_active                   ON salons(active);
CREATE INDEX idx_appointments_customer_id        ON appointments(customer_id);
CREATE INDEX idx_appointments_employee_id        ON appointments(employee_id);
CREATE INDEX idx_appointments_status             ON appointments(status);
CREATE INDEX idx_appointments_datetime           ON appointments(appointment_date_time);
CREATE INDEX idx_appointments_campaign_id        ON appointments(campaign_id);
CREATE INDEX idx_sales_salon_id                  ON sales(salon_id);
CREATE INDEX idx_sales_customer_id               ON sales(customer_id);
CREATE INDEX idx_sales_status                    ON sales(status);
CREATE INDEX idx_sales_completed_at              ON sales(completed_at);
CREATE INDEX idx_sale_items_sale_id               ON sale_items(sale_id);
CREATE INDEX idx_sale_items_employee_id           ON sale_items(employee_id);
CREATE INDEX idx_sale_payments_sale_id            ON sale_payments(sale_id);
CREATE INDEX idx_campaigns_code                  ON campaigns(code);
CREATE INDEX idx_campaigns_active                ON campaigns(active);
CREATE INDEX idx_campaigns_customer_id           ON campaigns(customer_id);
CREATE INDEX idx_campaigns_salon_id              ON campaigns(salon_id);
CREATE INDEX idx_salon_settings_salon_id         ON salon_settings(salon_id);
CREATE INDEX idx_salon_settings_key              ON salon_settings(setting_key);

ALTER TABLE users
    ADD CONSTRAINT fk_users_salon FOREIGN KEY (salon_id) REFERENCES salons(id),
    ADD CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees(id);
CREATE INDEX idx_users_salon_id ON users(salon_id);

CREATE TABLE user_salon_access (
    user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    salon_id BIGINT NOT NULL REFERENCES salons(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_salon_access UNIQUE (user_id, salon_id)
);
CREATE INDEX idx_user_salon_access_salon_id ON user_salon_access(salon_id);

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
    RAISE NOTICE '  - salon_settings';
    RAISE NOTICE '  - appointments';
    RAISE NOTICE '  - sales';
    RAISE NOTICE '  - sale_items';
    RAISE NOTICE '  - sale_payments';
END $$;
