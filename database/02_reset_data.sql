-- Xoa toan bo du lieu de ung dung sinh lai du lieu mau o lan chay tiep theo.
USE VietGoDB;
GO
DELETE FROM seat_locks;
DELETE FROM booking_fee_lines;
DELETE FROM tickets;
DELETE FROM bookings;
DELETE FROM consultations;
DELETE FROM train_coaches;
DELETE FROM train_trips;
DELETE FROM bus_trips;
DELETE FROM app_users;
GO
