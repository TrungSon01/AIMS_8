# Cấu hình Thông tin Ngân hàng cho VietQR

## Giải thích

Các thông tin trong `application.properties`:

```properties
vietqr.bank-code=MB
vietqr.bank-account=0358858860
vietqr.user-bank-name=NGUYEN TRUNG SON
```

**PHẢI LÀ** thông tin ngân hàng đã đăng ký với VietQR khi bạn tạo tài khoản/merchant.

## Tại sao quan trọng?

### 1. Khi tạo QR Code

Khi gọi API `generate-customer`, backend sẽ dùng các thông tin này để tạo QR code:

```java
VietQRGenerateRequest request = VietQRGenerateRequest.builder()
    .bankCode(vietQRConfig.getBankCode())        // "MB"
    .bankAccount(vietQRConfig.getBankAccount())   // "0358858860"
    .userBankName(vietQRConfig.getUserBankName()) // "NGUYEN TRUNG SON"
    .amount(...)
    .content(...)
    .build();
```

**→ QR code sẽ được tạo với thông tin ngân hàng này**

### 2. Khi nhận Callback

Khi VietQR gửi callback về, nó sẽ gửi thông tin giao dịch với:
- `bankAccount`: Số tài khoản nhận tiền (phải khớp với config)
- `bankCode`: Mã ngân hàng (phải khớp với config)
- `content`: Nội dung thanh toán (phải khớp với content khi tạo QR)
- `amount`: Số tiền (phải khớp với amount khi tạo QR)

**→ Backend sẽ tìm payment dựa trên các thông tin này**

## Vấn đề khi Test

Nếu bạn test callback với thông tin ngân hàng **KHÁC** với config:

```json
{
  "bankAccount": "8867699892",  // ❌ Khác với config "0358858860"
  "bankCode": "BIDV",            // ❌ Khác với config "MB"
  "content": "VQR26044A327PVJX THANH TOAN HOA DON",
  "amount": 150000,
  "transType": "C"
}
```

**→ Backend sẽ không tìm thấy payment vì:**
1. Payment được tạo với bank account "0358858860" (MB)
2. Callback có bank account "8867699892" (BIDV)
3. Không match được → Lỗi "Transaction not found"

## Cách Test Đúng

### Bước 1: Tạo Payment với VietQR

Gọi API tạo payment:
```bash
POST /api/payment/create
{
  "orderId": 1,
  "amount": 6.00,  // USD
  "description": "THANH TOAN HOA DON",
  "paymentMethod": "VIETQR"
}
```

Backend sẽ:
- Tạo QR code với bank account "0358858860" (MB)
- Lưu payment vào DB với description "THANH TOAN HOA DON"

### Bước 2: Test Callback với ĐÚNG thông tin

```bash
POST https://dev.vietqr.org/vqr/bank/api/test/transaction-callback
{
  "bankAccount": "0358858860",  // ✅ Khớp với config
  "bankCode": "MB",              // ✅ Khớp với config
  "content": "THANH TOAN HOA DON",  // ✅ Khớp với description khi tạo payment
  "amount": 150000,              // ✅ Khớp với amount (6 USD * 25000 = 150000 VND)
  "transType": "C"
}
```

**→ Backend sẽ tìm thấy payment và cập nhật status thành COMPLETED**

## Lưu ý

1. **Thông tin ngân hàng phải đúng:**
   - `bank-code`: Mã ngân hàng đã đăng ký với VietQR (ví dụ: "MB", "BIDV", "VCB", etc.)
   - `bank-account`: Số tài khoản đã đăng ký với VietQR
   - `user-bank-name`: Tên chủ tài khoản (viết hoa, không dấu)

2. **Khi test callback:**
   - Phải dùng **ĐÚNG** `bankAccount` và `bankCode` như trong config
   - Phải dùng **ĐÚNG** `content` và `amount` như khi tạo QR code

3. **Nếu muốn test với ngân hàng khác:**
   - Phải đăng ký ngân hàng đó với VietQR trước
   - Cập nhật config trong `application.properties`
   - Tạo payment mới với thông tin ngân hàng mới
   - Test callback với thông tin ngân hàng mới

## Checklist

- [ ] `bank-code` đúng với mã ngân hàng đã đăng ký với VietQR
- [ ] `bank-account` đúng với số tài khoản đã đăng ký với VietQR
- [ ] `user-bank-name` đúng với tên chủ tài khoản (viết hoa, không dấu)
- [ ] Khi test callback, dùng đúng `bankAccount` và `bankCode` như config
- [ ] Khi test callback, dùng đúng `content` và `amount` như khi tạo QR code
