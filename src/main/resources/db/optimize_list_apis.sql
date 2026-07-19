CREATE INDEX IF NOT EXISTS idx_sales_salon_status_created_at
    ON sales(salon_id, status, created_at DESC);;
CREATE INDEX IF NOT EXISTS idx_sales_completed_salon_date
    ON sales(salon_id, completed_at DESC) WHERE status = 'COMPLETED';;
CREATE INDEX IF NOT EXISTS idx_sales_completed_date
    ON sales(completed_at DESC) WHERE status = 'COMPLETED';;
CREATE INDEX IF NOT EXISTS idx_sale_items_employee_sale
    ON sale_items(employee_id, sale_id);;
CREATE INDEX IF NOT EXISTS idx_appointments_salon_status_date
    ON appointments(salon_id, status, appointment_date_time DESC);;
CREATE INDEX IF NOT EXISTS idx_appointments_employee_status_date
    ON appointments(employee_id, status, appointment_date_time DESC);;
