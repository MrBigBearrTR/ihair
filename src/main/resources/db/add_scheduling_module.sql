-- Salon programı ve randevu snapshot alanları. PostgreSQL üzerinde tekrar çalıştırılabilir.

CREATE TABLE IF NOT EXISTS salon_schedules (
    id          BIGSERIAL PRIMARY KEY,
    salon_id    BIGINT NOT NULL UNIQUE REFERENCES salons(id) ON DELETE CASCADE,
    time_zone   VARCHAR(255) NOT NULL DEFAULT 'Europe/Istanbul',
    configured  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);;

CREATE TABLE IF NOT EXISTS salon_schedule_days (
    id           BIGSERIAL PRIMARY KEY,
    schedule_id  BIGINT NOT NULL REFERENCES salon_schedules(id) ON DELETE CASCADE,
    day_of_week  VARCHAR(10) NOT NULL,
    closed       BOOLEAN NOT NULL DEFAULT FALSE,
    opens_at     TIME,
    closes_at    TIME,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP,
    CONSTRAINT uq_salon_schedule_day UNIQUE (schedule_id, day_of_week)
);;

CREATE TABLE IF NOT EXISTS salon_holidays (
    id          BIGSERIAL PRIMARY KEY,
    salon_id    BIGINT NOT NULL REFERENCES salons(id) ON DELETE CASCADE,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    CONSTRAINT chk_salon_holiday_range CHECK (end_date >= start_date)
);;

INSERT INTO salon_schedules (salon_id, time_zone, configured, created_at)
SELECT s.id, 'Europe/Istanbul', FALSE, NOW()
FROM salons s
ON CONFLICT (salon_id) DO NOTHING;;

INSERT INTO salon_schedule_days (schedule_id, day_of_week, closed, opens_at, closes_at, created_at)
SELECT ss.id, d.day_of_week, d.closed, d.opens_at, d.closes_at, NOW()
FROM salon_schedules ss
CROSS JOIN (VALUES
    ('MONDAY', FALSE, TIME '09:00', TIME '19:00'),
    ('TUESDAY', FALSE, TIME '09:00', TIME '19:00'),
    ('WEDNESDAY', FALSE, TIME '09:00', TIME '19:00'),
    ('THURSDAY', FALSE, TIME '09:00', TIME '19:00'),
    ('FRIDAY', FALSE, TIME '09:00', TIME '19:00'),
    ('SATURDAY', FALSE, TIME '09:00', TIME '19:00'),
    ('SUNDAY', TRUE, NULL, NULL)
) AS d(day_of_week, closed, opens_at, closes_at)
ON CONFLICT (schedule_id, day_of_week) DO NOTHING;;

ALTER TABLE appointments ADD COLUMN IF NOT EXISTS salon_id BIGINT;;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS duration_minutes_snapshot INTEGER;;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS ends_at TIMESTAMP;;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS schedule_overridden BOOLEAN NOT NULL DEFAULT FALSE;;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS schedule_override_reason VARCHAR(255);;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS schedule_override_by BIGINT;;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS schedule_override_at TIMESTAMP;;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;;

UPDATE appointments a
SET salon_id = e.salon_id
FROM employees e
WHERE a.employee_id = e.id
  AND a.salon_id IS NULL;;

UPDATE appointments a
SET duration_minutes_snapshot = hs.duration_minutes
FROM hair_services hs
WHERE a.hair_service_id = hs.id
  AND a.duration_minutes_snapshot IS NULL;;

UPDATE appointments
SET ends_at = appointment_date_time + (duration_minutes_snapshot * INTERVAL '1 minute')
WHERE ends_at IS NULL
  AND duration_minutes_snapshot IS NOT NULL;;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_appointments_salon') THEN
        ALTER TABLE appointments
            ADD CONSTRAINT fk_appointments_salon FOREIGN KEY (salon_id) REFERENCES salons(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_appointments_schedule_override_by') THEN
        ALTER TABLE appointments
            ADD CONSTRAINT fk_appointments_schedule_override_by
            FOREIGN KEY (schedule_override_by) REFERENCES users(id);
    END IF;
END $$;;

CREATE INDEX IF NOT EXISTS idx_appointments_salon_start
    ON appointments(salon_id, appointment_date_time);;
CREATE INDEX IF NOT EXISTS idx_salon_holidays_salon_range
    ON salon_holidays(salon_id, start_date, end_date);;
