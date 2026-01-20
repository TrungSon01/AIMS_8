-- Script để thêm các cột cần thiết cho chức năng PayPal vào bảng Payment
-- CHẠY SCRIPT NÀY TRONG MYSQL nếu database của bạn chưa có các cột này
-- Nếu database đã có đầy đủ các cột thì KHÔNG CẦN chạy script này

USE AIMS_DATABASE;

-- Kiểm tra và thêm các cột nếu chưa tồn tại
-- Lưu ý: MySQL không hỗ trợ IF NOT EXISTS trong ALTER TABLE ADD COLUMN
-- Nếu cột đã tồn tại, sẽ báo lỗi nhưng không sao, bạn có thể bỏ qua

ALTER TABLE Payment 
ADD COLUMN paymentCode VARCHAR(50) UNIQUE AFTER id;

ALTER TABLE Payment 
ADD COLUMN description TEXT AFTER amount;

ALTER TABLE Payment 
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING' AFTER description;

ALTER TABLE Payment 
ADD COLUMN qrCodeUrl TEXT AFTER status;

ALTER TABLE Payment 
ADD COLUMN expiresAt DATETIME AFTER createdAt;

ALTER TABLE Payment 
ADD COLUMN paidAt DATETIME AFTER expiresAt;

ALTER TABLE Payment 
ADD COLUMN paymentMethod VARCHAR(50) DEFAULT 'VIETQR' AFTER paidAt;

-- Thêm index để tìm kiếm nhanh hơn (bỏ qua nếu đã tồn tại)
CREATE INDEX idx_payment_code ON Payment(paymentCode);
CREATE INDEX idx_payment_status ON Payment(status);
CREATE INDEX idx_order_id ON Payment(orderId);
