# VietQR Callback Endpoint Documentation

## Endpoint

**URL:** `https://api.aims-group3.click/bank/api/transaction-sync`  
**Method:** `POST`  
**Content-Type:** `application/json`

## Mô tả

Endpoint này nhận callback từ VietQR khi có giao dịch thanh toán thành công. Sau khi nhận callback, backend sẽ:

1. Tìm payment tương ứng dựa trên thông tin từ callback
2. Đối chiếu thông tin (số tiền, nội dung, tài khoản ngân hàng)
3. Cập nhật trạng thái payment thành `COMPLETED`
4. Ghi log để phục vụ debug
5. Trả response về cho VietQR

## Request Body

```json
{
  "bankAccount": "0358858860",
  "content": "THANH TOAN HOA DON",
  "amount": 150000,
  "transType": "C",
  "bankCode": "MB",
  "transactionId": "optional",
  "transactionRefId": "optional",
  "orderId": "optional",
  "timestamp": "optional"
}
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `bankAccount` | String | Yes | Số tài khoản ngân hàng nhận tiền |
| `content` | String | Yes | Nội dung thanh toán |
| `amount` | BigDecimal | Yes | Số tiền thanh toán (VND) |
| `transType` | String | Yes | Loại giao dịch: "C" = Credit (nhận tiền) |
| `bankCode` | String | Yes | Mã ngân hàng (ví dụ: "MB") |
| `transactionId` | String | No | Transaction ID từ VietQR (nếu có) |
| `transactionRefId` | String | No | Transaction Reference ID từ VietQR (nếu có) |
| `orderId` | String | No | Order ID (nếu có) |
| `timestamp` | String | No | Timestamp của giao dịch (nếu có) |

## Response

### Success Response (HTTP 200)

```json
{
  "code": "00",
  "message": "Transaction processed successfully"
}
```

### Error Responses

#### Payment not found or validation failed (HTTP 200)

```json
{
  "code": "01",
  "message": "Transaction not found or validation failed"
}
```

#### Internal server error (HTTP 500)

```json
{
  "code": "99",
  "message": "Internal server error: [error message]"
}
```

## Response Codes

| Code | Description |
|------|-------------|
| `00` | Transaction processed successfully |
| `01` | Transaction not found or validation failed |
| `99` | Internal server error |

## Cách tìm Payment

Backend sẽ tìm payment theo thứ tự ưu tiên:

1. **Theo transactionId hoặc transactionRefId** (nếu có trong callback)
2. **Theo orderId** (nếu có trong callback) - tìm payment có:
   - `orderId` khớp
   - `paymentMethod = "VIETQR"`
   - `status = "PENDING"`
   - `amount` khớp (sau khi convert VND sang USD)
   - `description` chứa `content` từ callback
3. **Theo amount và content** - tìm payment có:
   - `paymentMethod = "VIETQR"`
   - `status = "PENDING"`
   - `amount` khớp (sau khi convert VND sang USD)
   - `description` chứa `content` từ callback
   - Ưu tiên payment có `bankAccount` trong description

## Validation

Backend sẽ đối chiếu:

1. **Payment Method**: Phải là `VIETQR`
2. **Amount**: Số tiền phải khớp (cho phép sai số 0.01 USD ~ 250 VND)
3. **Content**: Nội dung phải khớp (description có thể chứa thêm thông tin)
4. **TransType**: Phải là `"C"` (Credit - nhận tiền)
5. **Bank Account**: Kiểm tra nếu có trong description

## Test Endpoint

### 1. Test với VietQR Test API

Gửi request test đến VietQR để trigger callback:

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

VietQR sẽ giả lập giao dịch và gọi callback về endpoint của bạn.

### 2. Test trực tiếp endpoint callback

```bash
curl -X POST https://api.aims-group3.click/bank/api/transaction-sync \
  -H "Content-Type: application/json" \
  -d '{
    "bankAccount": "0358858860",
    "content": "Test Payment",
    "amount": 1250000,
    "transType": "C",
    "bankCode": "MB",
    "orderId": "1"
  }'
```

**Lưu ý:** 
- `amount` phải khớp với amount của payment (sau khi convert USD sang VND)
- `content` phải khớp với `description` của payment
- Payment phải có `status = "PENDING"` và `paymentMethod = "VIETQR"`

## Logs

Backend sẽ log chi tiết:

- Thông tin callback nhận được
- Kết quả tìm kiếm payment
- Kết quả validation
- Kết quả cập nhật payment

Xem logs trong console hoặc log files để debug.

## Notes

- Endpoint này là **public**, không cần authentication
- Chỉ xử lý payment có `status = "PENDING"`
- Nếu payment đã được xử lý (status khác PENDING), vẫn trả về success
- Amount được convert từ VND sang USD với tỷ giá trong config (mặc định 25000)
