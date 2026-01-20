# AIMSVER2 - Payment API Documentation

## Base URL
```
http://localhost:8080/api
```

---

## 1. Check Server Status

Kiểm tra server có đang chạy không.

**URL:** `/checkServer`  
**Method:** `GET`  
**Request Body:** Không có

### Response
```json
{
  "status": "success",
  "message": "Server is running!",
  "timestamp": "2026-01-20T15:30:00",
  "application": "AIMSVER2"
}
```

### Example
```bash
curl http://localhost:8080/api/checkServer
```

---

## 2. Create Payment

Tạo payment mới (PayPal hoặc VietQR).

**URL:** `/payment/create`  
**Method:** `POST`  
**Content-Type:** `application/json`

### Request Body
```json
{
  "orderId": 1,
  "amount": 50.00,
  "description": "Thanh toán đơn hàng #1",
  "paymentMethod": "PAYPAL",
  "returnUrl": "http://localhost:5173/success",
  "cancelUrl": "http://localhost:5173/cancel"
}
```

### Request Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | Integer | Yes | ID của đơn hàng (phải tồn tại trong database) |
| `amount` | BigDecimal | Yes | Số tiền thanh toán |
| `description` | String | Yes | Mô tả thanh toán |
| `paymentMethod` | String | Yes | Phương thức thanh toán: `PAYPAL` hoặc `VIETQR` |
| `returnUrl` | String | No | URL redirect sau khi thanh toán thành công (mặc định: `/api/payment/paypal/success`) |
| `cancelUrl` | String | No | URL redirect khi hủy thanh toán (mặc định: `/api/payment/paypal/cancel`) |

### Response (Success)
```json
{
  "paymentId": 1,
  "paymentCode": "PAYPAL-ABC12345",
  "status": "PENDING",
  "amount": 50.00,
  "description": "Thanh toán đơn hàng #1",
  "paymentMethod": "PAYPAL",
  "approvalUrl": "https://www.sandbox.paypal.com/checkoutnow?token=...",
  "transactionId": "PAYID-...",
  "createdAt": "2026-01-20T15:30:00",
  "expiresAt": null,
  "qrCodeUrl": null,
  "message": "Payment created successfully. Please approve the payment."
}
```

### Response (Error)
```json
{
  "status": "FAILED",
  "message": "Order not found: 999"
}
```

### Example
```bash
curl -X POST http://localhost:8080/api/payment/create \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 50.00,
    "description": "Test Payment",
    "paymentMethod": "PAYPAL"
  }'
```

---

## 3. PayPal Success Callback

Callback từ PayPal sau khi người dùng approve payment (tự động gọi bởi PayPal).

**URL:** `/payment/paypal/success`  
**Method:** `GET`  
**Query Parameters:**
- `paymentId` (required): Payment ID từ PayPal
- `PayerID` (required): Payer ID từ PayPal

### Response
```json
{
  "success": true,
  "message": "Payment completed successfully!",
  "payment": {
    "paymentId": 1,
    "paymentCode": "PAYPAL-ABC12345",
    "status": "COMPLETED",
    "transactionId": "PAYID-...",
    "message": "Payment completed"
  }
}
```

### Example
```
http://localhost:8080/api/payment/paypal/success?paymentId=PAYID-123&PayerID=ABC123
```

---

## 4. PayPal Cancel Callback

Callback từ PayPal khi người dùng hủy thanh toán (tự động gọi bởi PayPal).

**URL:** `/payment/paypal/cancel`  
**Method:** `GET`  
**Query Parameters:**
- `token` (optional): Payment token từ PayPal

### Response
```json
{
  "success": true,
  "message": "Payment cancelled by user",
  "payment": {
    "paymentId": 1,
    "status": "CANCELLED",
    "transactionId": "PAYID-...",
    "message": "Payment cancelled by user"
  }
}
```

### Example
```
http://localhost:8080/api/payment/paypal/cancel?token=PAYID-123
```

---

## 5. Confirm Payment (Manual)

Xác nhận payment thủ công (dùng cho testing hoặc webhook).

**URL:** `/payment/confirm`  
**Method:** `POST`  
**Content-Type:** `application/x-www-form-urlencoded` hoặc `application/json`

### Request Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `paymentId` | String | Yes | Payment ID từ PayPal |
| `payerId` | String | Yes | Payer ID từ PayPal |

### Response (Success)
```json
{
  "paymentId": 1,
  "paymentCode": "PAYPAL-ABC12345",
  "status": "COMPLETED",
  "transactionId": "PAYID-...",
  "message": "Payment completed"
}
```

### Response (Error)
```json
{
  "status": "FAILED",
  "message": "Payment not found: PAYID-123"
}
```

### Example
```bash
curl -X POST http://localhost:8080/api/payment/confirm \
  -d "paymentId=PAYID-123&payerId=ABC123"
```

---

## Payment Methods

### PayPal
- `paymentMethod`: `"PAYPAL"`
- Sau khi tạo payment, redirect user đến `approvalUrl` để approve payment
- PayPal sẽ tự động redirect về `returnUrl` hoặc `cancelUrl`

### VietQR
- `paymentMethod`: `"VIETQR"`
- Response sẽ có `qrCodeUrl` để hiển thị QR code
- User scan QR code để thanh toán

---

## Payment Status

- `PENDING`: Payment đã được tạo, chờ thanh toán
- `COMPLETED`: Payment đã hoàn thành
- `CANCELLED`: Payment đã bị hủy
- `FAILED`: Payment thất bại

---

## Error Codes

- `400 Bad Request`: Request không hợp lệ (thiếu thông tin, order không tồn tại, etc.)
- `500 Internal Server Error`: Lỗi server

---

## Notes

1. **Database Setup**: Đảm bảo đã chạy script `ALTER_PAYMENT_TABLE.sql` nếu database chưa có đầy đủ các cột
2. **PayPal Sandbox**: Đang sử dụng PayPal sandbox mode, cần có PayPal sandbox account để test
3. **CORS**: API đã được cấu hình CORS để cho phép request từ frontend
4. **Order ID**: Order ID phải tồn tại trong database trước khi tạo payment
