-- Script để thêm các cột cần thiết cho chức năng PayPal vào bảng Payment
-- Chạy script này thủ công trong MySQL nếu bạn muốn sử dụng đầy đủ chức năng PayPal
-- Nếu không chạy script này, ứng dụng vẫn hoạt động nhưng sẽ không lưu một số thông tin bổ sung

USE AIMS_DATABASE;

-- Kiểm tra và thêm các cột nếu chưa tồn tại
ALTER TABLE Payment 
ADD COLUMN IF NOT EXISTS paymentCode VARCHAR(50) UNIQUE AFTER id,
ADD COLUMN IF NOT EXISTS description TEXT AFTER amount,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING' AFTER description,
ADD COLUMN IF NOT EXISTS qrCodeUrl TEXT AFTER status,
ADD COLUMN IF NOT EXISTS expiresAt DATETIME AFTER createdAt,
ADD COLUMN IF NOT EXISTS paidAt DATETIME AFTER expiresAt,
ADD COLUMN IF NOT EXISTS paymentMethod VARCHAR(50) DEFAULT 'VIETQR' AFTER paidAt;

-- Thêm index để tìm kiếm nhanh hơn
CREATE INDEX IF NOT EXISTS idx_payment_code ON Payment(paymentCode);
CREATE INDEX IF NOT EXISTS idx_payment_status ON Payment(status);
CREATE INDEX IF NOT EXISTS idx_order_id ON Payment(orderId);
