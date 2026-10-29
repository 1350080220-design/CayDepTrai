-- =====================================================================
--  VietGo - Tao database tren SQL Server
--  Chay file nay MOT LAN bang SQL Server Management Studio (SSMS)
--  hoac Azure Data Studio truoc khi khoi dong ung dung.
--  Cac bang se do Hibernate tu tao (spring.jpa.hibernate.ddl-auto=update),
--  du lieu mau do DataSeeder tu sinh khi database con trong.
-- =====================================================================
IF DB_ID(N'VietGoDB') IS NULL
BEGIN
    CREATE DATABASE VietGoDB COLLATE Vietnamese_CI_AS;
END
GO

USE VietGoDB;
GO

-- (Tuy chon) Tao tai khoan dang nhap rieng cho ung dung thay vi dung 'sa'
-- CREATE LOGIN vietgo_app WITH PASSWORD = N'VietGo@2026', CHECK_POLICY = OFF;
-- CREATE USER vietgo_app FOR LOGIN vietgo_app;
-- ALTER ROLE db_owner ADD MEMBER vietgo_app;
GO
