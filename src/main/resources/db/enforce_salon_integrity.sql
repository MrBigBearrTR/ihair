-- Salon kapsamını kanonik appointments.salon_id üzerinden zorunlu kılar.
CREATE OR REPLACE FUNCTION ihair_validate_appointment_salon()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE linked_salon_id BIGINT;
BEGIN
    IF NEW.salon_id IS NULL THEN
        RAISE EXCEPTION 'appointments.salon_id zorunludur (appointment_id=%)', NEW.id;
    END IF;
    SELECT salon_id INTO linked_salon_id FROM customers WHERE id = NEW.customer_id;
    IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
        RAISE EXCEPTION 'Randevu-müşteri salon uyuşmazlığı (appointment_id=%)', NEW.id;
    END IF;
    SELECT salon_id INTO linked_salon_id FROM employees WHERE id = NEW.employee_id;
    IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
        RAISE EXCEPTION 'Randevu-çalışan salon uyuşmazlığı (appointment_id=%)', NEW.id;
    END IF;
    SELECT salon_id INTO linked_salon_id FROM hair_services WHERE id = NEW.hair_service_id;
    IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
        RAISE EXCEPTION 'Randevu-hizmet salon uyuşmazlığı (appointment_id=%)', NEW.id;
    END IF;
    IF NEW.campaign_id IS NOT NULL THEN
        SELECT salon_id INTO linked_salon_id FROM campaigns WHERE id = NEW.campaign_id;
        IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
            RAISE EXCEPTION 'Randevu-kampanya salon uyuşmazlığı (appointment_id=%)', NEW.id;
        END IF;
    END IF;
    RETURN NEW;
END
$$;;

CREATE OR REPLACE FUNCTION ihair_validate_sale_salon()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE linked_salon_id BIGINT;
BEGIN
    SELECT salon_id INTO linked_salon_id FROM customers WHERE id = NEW.customer_id;
    IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
        RAISE EXCEPTION 'Satış-müşteri salon uyuşmazlığı (sale_id=%)', NEW.id;
    END IF;
    IF NEW.source_appointment_id IS NOT NULL THEN
        SELECT salon_id INTO linked_salon_id FROM appointments WHERE id = NEW.source_appointment_id;
        IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
            RAISE EXCEPTION 'Satış-randevu salon uyuşmazlığı (sale_id=%)', NEW.id;
        END IF;
    END IF;
    IF NEW.campaign_id IS NOT NULL THEN
        SELECT salon_id INTO linked_salon_id FROM campaigns WHERE id = NEW.campaign_id;
        IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
            RAISE EXCEPTION 'Satış-kampanya salon uyuşmazlığı (sale_id=%)', NEW.id;
        END IF;
    END IF;
    RETURN NEW;
END
$$;;

CREATE OR REPLACE FUNCTION ihair_validate_sale_item_salon()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE sale_salon_id BIGINT; linked_salon_id BIGINT;
BEGIN
    SELECT salon_id INTO sale_salon_id FROM sales WHERE id = NEW.sale_id;
    SELECT salon_id INTO linked_salon_id FROM employees WHERE id = NEW.employee_id;
    IF linked_salon_id IS DISTINCT FROM sale_salon_id THEN
        RAISE EXCEPTION 'Satış kalemi-çalışan salon uyuşmazlığı (sale_item_id=%)', NEW.id;
    END IF;
    SELECT salon_id INTO linked_salon_id FROM hair_services WHERE id = NEW.hair_service_id;
    IF linked_salon_id IS DISTINCT FROM sale_salon_id THEN
        RAISE EXCEPTION 'Satış kalemi-hizmet salon uyuşmazlığı (sale_item_id=%)', NEW.id;
    END IF;
    RETURN NEW;
END
$$;;

CREATE OR REPLACE FUNCTION ihair_validate_campaign_salon()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE customer_salon_id BIGINT;
BEGIN
    IF NEW.customer_id IS NOT NULL THEN
        SELECT salon_id INTO customer_salon_id FROM customers WHERE id = NEW.customer_id;
        IF customer_salon_id IS DISTINCT FROM NEW.salon_id THEN
            RAISE EXCEPTION 'Kampanya-müşteri salon uyuşmazlığı (campaign_id=%)', NEW.id;
        END IF;
    END IF;
    RETURN NEW;
END
$$;;

CREATE OR REPLACE FUNCTION ihair_validate_redemption_salon()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE linked_salon_id BIGINT;
BEGIN
    SELECT salon_id INTO linked_salon_id FROM sales WHERE id = NEW.sale_id;
    IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
        RAISE EXCEPTION 'Redemption-satış salon uyuşmazlığı (redemption_id=%)', NEW.id;
    END IF;
    SELECT salon_id INTO linked_salon_id FROM campaigns WHERE id = NEW.campaign_id;
    IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
        RAISE EXCEPTION 'Redemption-kampanya salon uyuşmazlığı (redemption_id=%)', NEW.id;
    END IF;
    SELECT salon_id INTO linked_salon_id FROM customers WHERE id = NEW.customer_id;
    IF linked_salon_id IS DISTINCT FROM NEW.salon_id THEN
        RAISE EXCEPTION 'Redemption-müşteri salon uyuşmazlığı (redemption_id=%)', NEW.id;
    END IF;
    RETURN NEW;
END
$$;;

DROP TRIGGER IF EXISTS trg_appointments_same_salon ON appointments;;
CREATE CONSTRAINT TRIGGER trg_appointments_same_salon
AFTER INSERT OR UPDATE ON appointments
DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW
EXECUTE FUNCTION ihair_validate_appointment_salon();;
DROP TRIGGER IF EXISTS trg_sales_same_salon ON sales;;
CREATE CONSTRAINT TRIGGER trg_sales_same_salon
AFTER INSERT OR UPDATE ON sales
DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW
EXECUTE FUNCTION ihair_validate_sale_salon();;
DROP TRIGGER IF EXISTS trg_sale_items_same_salon ON sale_items;;
CREATE CONSTRAINT TRIGGER trg_sale_items_same_salon
AFTER INSERT OR UPDATE ON sale_items
DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW
EXECUTE FUNCTION ihair_validate_sale_item_salon();;
DROP TRIGGER IF EXISTS trg_campaigns_same_salon ON campaigns;;
CREATE CONSTRAINT TRIGGER trg_campaigns_same_salon
AFTER INSERT OR UPDATE ON campaigns
DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW
EXECUTE FUNCTION ihair_validate_campaign_salon();;
DROP TRIGGER IF EXISTS trg_campaign_redemptions_same_salon ON campaign_redemptions;;
CREATE CONSTRAINT TRIGGER trg_campaign_redemptions_same_salon
AFTER INSERT OR UPDATE ON campaign_redemptions
DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW
EXECUTE FUNCTION ihair_validate_redemption_salon();;

CREATE OR REPLACE FUNCTION ihair_block_customer_salon_move()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.salon_id IS DISTINCT FROM OLD.salon_id AND (
        EXISTS (SELECT 1 FROM appointments WHERE customer_id = OLD.id)
        OR EXISTS (SELECT 1 FROM sales WHERE customer_id = OLD.id)
        OR EXISTS (SELECT 1 FROM campaign_redemptions WHERE customer_id = OLD.id)
    ) THEN
        RAISE EXCEPTION 'Bağlı hareketi bulunan müşteri başka salona taşınamaz (customer_id=%)', OLD.id;
    END IF;
    RETURN NEW;
END
$$;;
CREATE OR REPLACE FUNCTION ihair_block_employee_salon_move()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.salon_id IS DISTINCT FROM OLD.salon_id AND (
        EXISTS (SELECT 1 FROM appointments WHERE employee_id = OLD.id)
        OR EXISTS (SELECT 1 FROM sale_items WHERE employee_id = OLD.id)
    ) THEN
        RAISE EXCEPTION 'Bağlı hareketi bulunan çalışan başka salona taşınamaz (employee_id=%)', OLD.id;
    END IF;
    RETURN NEW;
END
$$;;
CREATE OR REPLACE FUNCTION ihair_block_service_salon_move()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.salon_id IS DISTINCT FROM OLD.salon_id AND (
        EXISTS (SELECT 1 FROM appointments WHERE hair_service_id = OLD.id)
        OR EXISTS (SELECT 1 FROM sale_items WHERE hair_service_id = OLD.id)
    ) THEN
        RAISE EXCEPTION 'Bağlı hareketi bulunan hizmet başka salona taşınamaz (service_id=%)', OLD.id;
    END IF;
    RETURN NEW;
END
$$;;
DROP TRIGGER IF EXISTS trg_customers_block_salon_move ON customers;;
CREATE TRIGGER trg_customers_block_salon_move BEFORE UPDATE OF salon_id ON customers
FOR EACH ROW EXECUTE FUNCTION ihair_block_customer_salon_move();;
DROP TRIGGER IF EXISTS trg_employees_block_salon_move ON employees;;
CREATE TRIGGER trg_employees_block_salon_move BEFORE UPDATE OF salon_id ON employees
FOR EACH ROW EXECUTE FUNCTION ihair_block_employee_salon_move();;
DROP TRIGGER IF EXISTS trg_hair_services_block_salon_move ON hair_services;;
CREATE TRIGGER trg_hair_services_block_salon_move BEFORE UPDATE OF salon_id ON hair_services
FOR EACH ROW EXECUTE FUNCTION ihair_block_service_salon_move();;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM appointments WHERE salon_id IS NULL) THEN
        ALTER TABLE appointments ALTER COLUMN salon_id SET NOT NULL;
    END IF;
END
$$;;
