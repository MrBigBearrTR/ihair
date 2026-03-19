-- ============================================================
-- iHair Projesi - PostgreSQL Tablo Oluşturma Script'i
-- Tüm tablolar ve kolonlar için açıklama (COMMENT ON) içerir.
-- ============================================================

-- Mevcut tabloları temizle (geliştirme ortamı için)
DROP TABLE IF EXISTS appointments CASCADE;
DROP TABLE IF EXISTS hair_services CASCADE;
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS salons CASCADE;

-- ============================================================
-- SALONS - Kuaför Salonları
-- ============================================================
CREATE TABLE salons (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    address     VARCHAR(500),
    phone       VARCHAR(20),
    email       VARCHAR(255),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

COMMENT ON TABLE  salons             IS 'Sisteme kayıtlı kuaför salonlarını tutar';
COMMENT ON COLUMN salons.id          IS 'Birincil anahtar, otomatik artan';
COMMENT ON COLUMN salons.name        IS 'Salonun ticari adı';
COMMENT ON COLUMN salons.address     IS 'Salonun açık adresi';
COMMENT ON COLUMN salons.phone       IS 'Salonun telefon numarası';
COMMENT ON COLUMN salons.email       IS 'Salonun e-posta adresi';
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
COMMENT ON COLUMN customers.active      IS 'Müşterinin aktiflik durumu; false ise pasif müşteri olarak işaretlenir';
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
COMMENT ON COLUMN hair_services.price             IS 'Hizmetin fiyatı (TL cinsinden, 2 ondalık)';
COMMENT ON COLUMN hair_services.duration_minutes  IS 'Hizmetin tahmini süresi (dakika cinsinden)';
COMMENT ON COLUMN hair_services.salon_id          IS 'Hizmetin ait olduğu salonun yabancı anahtarı';
COMMENT ON COLUMN hair_services.created_at        IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN hair_services.updated_at        IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- APPOINTMENTS - Randevular
-- ============================================================
CREATE TABLE appointments (
    id                      BIGSERIAL       PRIMARY KEY,
    customer_id             BIGINT          NOT NULL,
    employee_id             BIGINT          NOT NULL,
    hair_service_id         BIGINT          NOT NULL,
    appointment_date_time   TIMESTAMP       NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    notes                   TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,

    CONSTRAINT fk_appointments_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_appointments_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_appointments_hair_service
        FOREIGN KEY (hair_service_id) REFERENCES hair_services(id),
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
COMMENT ON COLUMN appointments.created_at             IS 'Kaydın oluşturulma tarihi (otomatik)';
COMMENT ON COLUMN appointments.updated_at             IS 'Kaydın son güncellenme tarihi (otomatik)';

-- ============================================================
-- İNDEKSLER - Sık sorgulanan kolonlar için performans
-- ============================================================
CREATE INDEX idx_employees_salon_id         ON employees(salon_id);
CREATE INDEX idx_hair_services_salon_id     ON hair_services(salon_id);
CREATE INDEX idx_appointments_customer_id   ON appointments(customer_id);
CREATE INDEX idx_appointments_employee_id   ON appointments(employee_id);
CREATE INDEX idx_appointments_status        ON appointments(status);
CREATE INDEX idx_appointments_datetime      ON appointments(appointment_date_time);

-- ============================================================
-- Başarı mesajı
-- ============================================================
DO $$
BEGIN
    RAISE NOTICE 'iHair tabloları ve açıklamaları başarıyla oluşturuldu.';
    RAISE NOTICE '  - salons';
    RAISE NOTICE '  - employees';
    RAISE NOTICE '  - customers';
    RAISE NOTICE '  - hair_services';
    RAISE NOTICE '  - appointments';
END $$;
