DROP DATABASE AIMS_DATABASE;
CREATE DATABASE IF NOT EXISTS AIMS_DATABASE;
USE AIMS_DATABASE;

CREATE TABLE Product (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    value DECIMAL(10,2),
    imgUrl TEXT
);

CREATE TABLE Book (
    id INT PRIMARY KEY,
    author VARCHAR(255),
    coverType VARCHAR(100),
    publisher VARCHAR(255),
    publishDate DATE,
    totalPage INT,
    language VARCHAR(100),
    genre VARCHAR(100),
    FOREIGN KEY (id) REFERENCES Product(id) ON DELETE CASCADE
);

CREATE TABLE CD (
    id INT PRIMARY KEY,
    musicType VARCHAR(100),
    recordLabel VARCHAR(255),
    artist VARCHAR(255),
    releaseDate DATE,
    FOREIGN KEY (id) REFERENCES Product(id) ON DELETE CASCADE
);

CREATE TABLE DVD (
    id INT PRIMARY KEY,
    discType VARCHAR(100),
    director VARCHAR(255),
    runtime INT, -- Thời lượng phim (phút)
    studio VARCHAR(255),
    subtitle VARCHAR(255),
    releaseDate DATE,
    FOREIGN KEY (id) REFERENCES Product(id) ON DELETE CASCADE
);

CREATE TABLE Newspaper (
    id INT PRIMARY KEY,
    publisher VARCHAR(255),
    publishDate DATE,
    issueNumber INT,
    editorInChief VARCHAR(255), -- Sửa lại tên cột cho đúng
    section VARCHAR(255),
    language VARCHAR(100),
    FOREIGN KEY (id) REFERENCES Product(id) ON DELETE CASCADE
);

CREATE TABLE `Order` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customerName VARCHAR(255) NOT NULL,
    addressLine VARCHAR(500),
    shippingFee DECIMAL(10,2) DEFAULT 0,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    price DECIMAL(10,2) -- Tổng giá trị đơn hàng
);

CREATE TABLE OrderProduct (
    orderId INT,
    productId INT,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL, -- Giá tại thời điểm đặt hàng
    PRIMARY KEY (orderId, productId),
    FOREIGN KEY (orderId) REFERENCES `Order`(id) ON DELETE CASCADE,
    FOREIGN KEY (productId) REFERENCES Product(id) ON DELETE CASCADE
);


CREATE TABLE Payment (
    id INT PRIMARY KEY AUTO_INCREMENT,
    orderId INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transactionId VARCHAR(255) UNIQUE,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (orderId) REFERENCES `Order`(id) ON DELETE CASCADE
);

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

-- PRODUCT
INSERT INTO Product (title, category, price, quantity, value, imgUrl) VALUES
('Clean Code', 'BOOK', 150000, 10, 150000, 'https://example.com/clean-code.jpg'),
('Greatest Hits', 'CD', 120000, 20, 120000, 'https://example.com/greatest-hits.jpg'),
('Inception', 'DVD', 180000, 15, 180000, 'https://example.com/inception.jpg');

-- BOOK (id = 1)
INSERT INTO Book (id, author, coverType, publisher, publishDate, totalPage, language, genre)
VALUES (1, 'Robert C. Martin', 'Hardcover', 'Prentice Hall', '2008-08-01', 464, 'English', 'Programming');

-- CD (id = 2)
INSERT INTO CD (id, musicType, recordLabel, artist, releaseDate)
VALUES (2, 'Pop', 'Sony Music', 'Various Artists', '2020-05-10');

-- DVD (id = 3)
INSERT INTO DVD (id, discType, director, runtime, studio, subtitle, releaseDate)
VALUES (3, 'Blu-ray', 'Christopher Nolan', 148, 'Warner Bros', 'English, Vietnamese', '2010-07-16');
INSERT INTO `Order` (customerName, addressLine, shippingFee, price)
VALUES ('Nguyen Trung Son', 'Ha Noi, Viet Nam', 30000, 450000);
INSERT INTO OrderProduct (orderId, productId, quantity, price) VALUES
(1, 1, 1, 150000), -- 1 sách Clean Code
(1, 2, 1, 120000), -- 1 CD
(1, 3, 1, 150000); -- DVD đang giảm giá tại thời điểm mua
INSERT INTO Payment (
    orderId,
    amount,
    transactionId,
    paymentCode,
    description,
    status,
    paymentMethod,
    paidAt
)
VALUES (
    1,
    450000,
    'PAYPAL-TRANSACTION-9X82AB',
    'PAY-20260120-0001',
    'Thanh toán đơn hàng #1 qua PayPal',
    'COMPLETED',
    'PAYPAL',
    NOW()
);
