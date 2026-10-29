-- Mot so truy van kiem tra nhanh khi demo
USE VietGoDB;
GO
-- Don dat ve moi nhat va trang thai
SELECT TOP 20 code, ticket_type, status, contact_name, passenger_count, total_amount, refund_amount, created_at
FROM bookings ORDER BY created_at DESC;

-- Doanh thu rong theo loai ve
SELECT ticket_type, COUNT(*) AS so_don, SUM(total_amount - refund_amount) AS doanh_thu_rong
FROM bookings WHERE paid_at IS NOT NULL GROUP BY ticket_type;

-- Ghe dang bi khoa (giu cho / da ban) theo chuyen
SELECT ticket_type, trip_id, COUNT(*) AS so_ghe FROM seat_locks GROUP BY ticket_type, trip_id ORDER BY so_ghe DESC;

-- Lich tu van sap toi
SELECT appointment_time, full_name, phone, ticket_type, channel, topic, status
FROM consultations WHERE appointment_time >= SYSDATETIME() ORDER BY appointment_time;
GO
