# Hướng dẫn Test VietQR Callback và Xử lý Lỗi 400/E76

## Lỗi 400 Bad Request - Nguyên nhân và Cách xử lý

Lỗi **400 Bad Request** từ VietQR test API có thể do các nguyên nhân sau:

### 1. transType không đúng

**Nguyên nhân:** `transType` phải là `"C"` (Credit - nhận tiền), không phải `"D"` (Debit - chi tiền).

**Giải pháp:** Đổi `transType` thành `"C"`:

```json
{
  "transType": "C"  // ✅ Đúng - Credit (nhận tiền)
}
```

**Lưu ý:**
- `"C"` = Credit (nhận tiền vào tài khoản) - Dùng cho thanh toán
- `"D"` = Debit (chi tiền từ tài khoản) - Không dùng cho test callback

### 2. Content và Amount không khớp với QR Code đã tạo

**Nguyên nhân:** Theo documentation VietQR:
- `content` phải **đúng với nội dung từ Response API khi tạo mã QR**
- `amount` phải **đúng với số tiền từ Response API khi tạo mã QR** (với dynamic QR - qrType = 0)

**Giải pháp:** 
1. Lấy `content` và `amount` từ response khi tạo QR code
2. Sử dụng chính xác các giá trị đó trong test callback

**Ví dụ:**
- Khi tạo QR code, response có: `"content": "THANH TOAN HOA DON"`, `"amount": "150000"`
- Khi test callback, phải dùng chính xác: `"content": "THANH TOAN HOA DON"`, `"amount": 150000`

### 2. Content không hợp lệ

**Yêu cầu của VietQR:**
- **Tối đa 23 ký tự**
- **Tiếng Việt không dấu** (không có dấu như ắ, ằ, ẳ, ẵ, ặ, đ, ế, ề, ể, ễ, ệ, etc.)
- **Không có ký tự đặc biệt**

**Ví dụ:**
- ❌ `"THANH TOÁN HÓA ĐƠN"` - Có dấu
- ❌ `"THANH TOAN HOA DON #123"` - Có ký tự đặc biệt
- ✅ `"THANH TOAN HOA DON"` - Đúng format

### 3. Request Body đầy đủ (theo Documentation VietQR)

Theo documentation chính thức, request body chỉ cần các field sau:

```json
{
  "bankAccount": "0358858860",
  "content": "THANH TOAN HOA DON",
  "amount": 150000,
  "transType": "C",
  "bankCode": "MB"
}
```

**Lưu ý:**
- `callbackUrl` **KHÔNG** cần trong request body (có thể được cấu hình ở VietQR dashboard hoặc tự động)
- `content` và `amount` phải khớp với response khi tạo QR code
- `transType` mặc định là `"C"` (có thể là `"D"` hoặc `"C"`)

## Cách Test với Postman

### Bước 1: Chuẩn bị Request

**Method:** `POST`  
**URL:** `https://dev.vietqr.org/vqr/bank/api/test/transaction-callback`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "bankAccount": "0358858860",
  "content": "THANH TOAN HOA DON",
  "amount": 150000,
  "transType": "C",
  "bankCode": "MB"
}
```

**⚠️ QUAN TRỌNG:**
- `transType` mặc định là `"C"` (Credit - nhận tiền), có thể là `"D"` (Debit) hoặc `"C"`
- `content` và `amount` phải **khớp chính xác** với response khi tạo QR code
- `callbackUrl` **KHÔNG** cần trong request body

### Bước 2: Gửi Request

Sau khi gửi request, VietQR sẽ:
1. Nhận request test
2. Giả lập giao dịch thành công
3. Gọi callback về `callbackUrl` của bạn

### Bước 3: Kiểm tra Callback

Kiểm tra logs của backend để xem callback có được nhận không:

```
=== VietQR Callback Received ===
Bank Account: 0358858860
Amount: 150000 VND
Content: THANH TOAN HOA DON
...
```

## Test với cURL

```bash
curl -X POST https://dev.vietqr.org/vqr/bank/api/test/transaction-callback \
  -H "Content-Type: application/json" \
  -d '{
    "bankAccount": "0358858860",
    "content": "THANH TOAN HOA DON",
    "amount": 150000,
    "transType": "C",
    "bankCode": "MB"
  }'
```

**Lưu ý:** `content` và `amount` phải khớp với response khi tạo QR code!

## Các Lỗi Thường Gặp

### 400 Bad Request

**Nguyên nhân:**
- `transType` không đúng (phải là `"C"`, không phải `"D"`)
- Thiếu field bắt buộc
- Format request không đúng
- `callbackUrl` không hợp lệ

**Giải pháp:**
- Đảm bảo `transType = "C"`
- Kiểm tra đầy đủ các field bắt buộc
- Kiểm tra format JSON
- Đảm bảo `callbackUrl` là URL hợp lệ

### E76 - Invalid Request

**Nguyên nhân:**
- Thiếu `callbackUrl`
- `content` có dấu tiếng Việt
- `content` quá dài (> 23 ký tự)
- Format request không đúng

**Giải pháp:**
- Thêm `callbackUrl` vào request
- Đảm bảo `content` không dấu và ≤ 23 ký tự
- Kiểm tra format JSON

### E77 - Invalid Callback URL

**Nguyên nhân:**
- `callbackUrl` không hợp lệ
- `callbackUrl` không accessible từ internet

**Giải pháp:**
- Đảm bảo `callbackUrl` là URL hợp lệ (bắt đầu với `http://` hoặc `https://`)
- Đảm bảo endpoint callback của bạn đang chạy và accessible từ internet

### E78 - Invalid Bank Account

**Nguyên nhân:**
- `bankAccount` không đúng format
- `bankAccount` không khớp với `bankCode`

**Giải pháp:**
- Kiểm tra `bankAccount` và `bankCode` có khớp không
- Đảm bảo `bankAccount` là số tài khoản hợp lệ

## Checklist trước khi Test

- [ ] `transType` = `"C"` hoặc `"D"` (mặc định là `"C"`)
- [ ] `content` **khớp chính xác** với `content` từ response khi tạo QR code
- [ ] `amount` **khớp chính xác** với `amount` từ response khi tạo QR code (với dynamic QR)
- [ ] `content` không có dấu tiếng Việt
- [ ] `content` ≤ 23 ký tự
- [ ] `amount` là số nguyên (Long, VND)
- [ ] `bankCode` và `bankAccount` khớp với config
- [ ] Endpoint callback đang chạy và accessible từ internet
- [ ] Backend đã có payment tương ứng với thông tin test

## Debug Tips

1. **Kiểm tra Response từ VietQR Test API:**
   - Nếu thành công, sẽ trả về status 200
   - Nếu lỗi, sẽ trả về error code (E76, E77, etc.)

2. **Kiểm tra Logs Backend:**
   - Xem callback có được nhận không
   - Xem payment có được tìm thấy không
   - Xem validation có pass không

3. **Test trực tiếp Callback Endpoint:**
   ```bash
   curl -X POST https://api.aims-group3.click/bank/api/transaction-sync \
     -H "Content-Type: application/json" \
     -d '{
       "bankAccount": "0358858860",
       "content": "THANH TOAN HOA DON",
       "amount": 150000,
       "transType": "C",
       "bankCode": "MB"
     }'
   ```

## Notes

- VietQR test API chỉ hoạt động với `dev.vietqr.org`
- Production API sẽ là `api.vietqr.org`
- Callback URL phải là HTTPS (khuyến nghị) hoặc HTTP
- Đảm bảo endpoint callback trả về HTTP 200 với response format đúng
