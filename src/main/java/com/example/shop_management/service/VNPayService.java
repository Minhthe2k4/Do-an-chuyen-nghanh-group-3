package com.example.shop_management.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.example.shop_management.service.VNPayUtils.hmacSHA512;

@Service
public class VNPayService {

    // Mã website được cấp bởi VNPay
    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    // Khóa bí mật (dùng để tạo chữ ký bảo mật)
    @Getter
    @Value("${vnpay.hashSecret}")
    private String secretKey;

    // URL của VNPay để tạo giao dịch
    @Value("${vnpay.payUrl}")
    private String vnp_Url;

    // URL hệ thống của bạn để VNPay redirect về sau khi thanh toán
    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    /**
     * Hàm tạo URL thanh toán cho VNPay
     */
    public String createOrder(int amount, String orderInfo, String ipAddress) throws UnsupportedEncodingException {
        String vnp_Version = "2.1.0";       // Phiên bản API
        String vnp_Command = "pay";         // Lệnh thanh toán
        String vnp_OrderInfo = orderInfo;   // Nội dung đơn hàng
        String orderType = "other";         // Loại hàng hóa/dịch vụ

        // Mã giao dịch duy nhất (dùng timestamp để tránh trùng lặp)
        String vnp_TxnRef = String.valueOf(System.currentTimeMillis());
        String vnp_IpAddr = ipAddress;

        // Ngày tạo giao dịch
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());

        // Ngày hết hạn (sau 15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        // Tham số bắt buộc gửi lên VNPay
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu số tiền *100
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Lấy danh sách key và sắp xếp theo alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder(); // dữ liệu để tạo chữ ký
        StringBuilder query = new StringBuilder();    // query string để gửi VNPay

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Thêm vào chuỗi hashData (không encode key nhưng encode value)
                hashData.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()))
                        .append('&');

                // Thêm vào chuỗi query (cả key và value đều phải encode)
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()))
                        .append('&');
            }
        }

        // Xóa ký tự '&' cuối cùng
        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);

        // Sinh chữ ký bảo mật (HMAC SHA512)
        String vnp_SecureHash = hmacSHA512(secretKey, hashData.toString());

        // Thêm chữ ký vào query
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        // Trả về URL hoàn chỉnh để redirect người dùng sang VNPay
        return vnp_Url + "?" + query.toString();
    }

    /**
     * Hàm lấy toàn bộ tham số trả về từ VNPay (sau khi người dùng thanh toán xong)
     */
    public Map<String, String> extractVnpParams(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        // Duyệt qua toàn bộ parameter từ request
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            String value = (values != null && values.length > 0) ? values[0] : "";
            fields.put(key, value);
        }
        return fields;
    }

}
