-- Mevcut PostgreSQL kurulumları için geriye uyumlu, veri kaybetmeyen salon kapsamı geçişi.
-- Eski kayıtlar NULL bırakılır; salon ataması yapılana kadar yalnız ADMIN tarafından görülebilir.
ALTER TABLE users ADD COLUMN IF NOT EXISTS salon_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS employee_id BIGINT;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS salon_id BIGINT;
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS salon_id BIGINT;
ALTER TABLE hair_services ALTER COLUMN duration_minutes SET DEFAULT 30;

CREATE TABLE IF NOT EXISTS user_salon_access (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    salon_id BIGINT NOT NULL REFERENCES salons(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_salon_access UNIQUE (user_id, salon_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_employee_id ON users(employee_id) WHERE employee_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_salon_id ON users(salon_id);
CREATE INDEX IF NOT EXISTS idx_user_salon_access_salon_id ON user_salon_access(salon_id);
CREATE INDEX IF NOT EXISTS idx_customers_salon_id ON customers(salon_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_salon_id ON campaigns(salon_id);

-- EMPLOYEE kapsamının tek kaynağı employee.salon'dur.
UPDATE users u
SET salon_id = e.salon_id
FROM employees e
WHERE u.role = 'EMPLOYEE'
  AND u.employee_id = e.id
  AND u.salon_id IS DISTINCT FROM e.salon_id;

-- Eski varsayılan salonu ve çalışan salonunu çoklu yetki tablosuna taşı.
INSERT INTO user_salon_access (user_id, salon_id)
SELECT u.id, u.salon_id
FROM users u
WHERE u.salon_id IS NOT NULL
ON CONFLICT (user_id, salon_id) DO NOTHING;
