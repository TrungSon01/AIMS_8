# Hướng dẫn Test VietQR - Flow đúng

## ⚠️ Vấn đề thường gặp

Khi bạn generate QR code **trực tiếp qua Postman** (không qua backend), payment **KHÔNG được tạo trong DB**. Khi test callback, backend sẽ không tìm thấy payment → Lỗi "Transaction not found".

## ✅ Flow Test Đúng

### Bước 1: Tạo Payment qua Backend API

**KHÔNG** generate QR code trực tiếp qua Postman. Thay vào đó, tạo payment qua backend API:

```bash
POST http://localhost:8080/api/payment/create
Content-Type: application/json

{
  "orderId": 1,
  "amount": 6.00,
  "description": "THANH TOAN HOA DON",
  "paymentMethod": "VIETQR"
}
```

**Response sẽ có:**
```json
{
  "paymentId": 1,
  "paymentCode": "VIETQR-ABC12345",
  "status": "PENDING",
  "amount": 6.00,
  "description": "THANH TOAN HOA DON",
  "transactionId": "MGMwZWJlYzUtZDJjOSOOMTExLTg4YmUtYjYzMmU3YjQ3NWIx",
  "qrCode": "00020101021238570010...",
  "qrLink": "https://pro.vietqr.vn/qr-generated?token=...",
  ...
}
```

**→ Payment đã được lưu vào DB với:**
- `amount = 6.00 USD`
- `description = "THANH TOAN HOA DON"`
- `transactionId = "MGMwZWJlYzUtZDJjOSOOMTExLTg4YmUtYjYzMmU3YjQ3NWIx"`
- `status = "PENDING"`

### Bước 2: Test Callback với ĐÚNG thông tin

Lấy thông tin từ response ở Bước 1:

```bash
POST https://dev.vietqr.org/vqr/bank/api/test/transaction-callback
Content-Type: application/json

{
  "bankAccount": "8867699892",  // Từ response generate QR
  "bankCode": "BIDV",            // Từ response generate QR
  "content": "VQR26044A5CCKYZA THANH TOAN HOA DON",  // Từ response generate QR (có prefix)
  "amount": 150000,              // 6 USD * 25000 = 150000 VND
  "transType": "C"
}
```

**Lưu ý:**
- `content` từ response có prefix "VQR26044A5CCKYZA" → Backend sẽ tự động extract phần "THANH TOAN HOA DON"
- `amount` = 150000 VND (6 USD * 25000)
- `bankAccount` và `bankCode` phải khớp với thông tin khi generate QR

**→ Backend sẽ:**
1. Extract content: "VQR26044A5CCKYZA THANH TOAN HOA DON" → "THANH TOAN HOA DON"
2. Tìm payment với description = "THANH TOAN HOA DON" và amount = 6.00 USD
3. Cập nhật status = "COMPLETED"

## 🔍 Debug khi gặp lỗi

### Kiểm tra Payment trong DB

Backend sẽ log tất cả payments VIETQR PENDING khi không tìm thấy:

```
WARN - Payment not found for callback: bankAccount=8867699892, amount=150000, content=VQR26044A5CCKYZA THANH TOAN HOA DON
WARN - Available PENDING VIETQR payments in DB: 2
WARN -   - Payment ID: 1, Amount: 6.00 USD, Description: THANH TOAN HOA DON, TransactionId: MGMwZWJlYzUtZDJjOSOOMTExLTg4YmUtYjYzMmU3YjQ3NWIx
WARN -   - Payment ID: 2, Amount: 10.00 USD, Description: Test Payment, TransactionId: ...
```

### Kiểm tra Content Matching

Backend sẽ log quá trình extract và so sánh content:

```
INFO - Extracted content: 'VQR26044A5CCKYZA THANH TOAN HOA DON' -> 'THANH TOAN HOA DON'
INFO - Content matched: callback=VQR26044A5CCKYZA THANH TOAN HOA DON, extracted=THANH TOAN HOA DON, payment=THANH TOAN HOA DON
```

## 📝 Checklist

- [ ] **Tạo payment qua backend API** (không generate QR trực tiếp)
- [ ] Lưu `transactionId` từ response
- [ ] Lưu `content` từ response (có prefix)
- [ ] Lưu `amount` từ response (VND)
- [ ] Test callback với đúng `content`, `amount`, `bankAccount`, `bankCode`
- [ ] Kiểm tra logs backend để xem payment có được tìm thấy không

## 🚫 Không làm

- ❌ Generate QR code trực tiếp qua Postman mà không tạo payment qua backend
- ❌ Test callback với thông tin khác với khi generate QR
- ❌ Dùng `bankAccount` hoặc `bankCode` khác với config

## ✅ Nên làm

- ✅ Luôn tạo payment qua backend API trước
- ✅ Dùng đúng thông tin từ response generate QR để test callback
- ✅ Kiểm tra logs backend để debug
- ✅ Đảm bảo `bankAccount` và `bankCode` khớp với config
