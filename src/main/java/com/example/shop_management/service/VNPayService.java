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

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Getter
    @Value("${vnpay.hashSecret}")
    private String secretKey;

    @Value("${vnpay.payUrl}")
    private String vnp_Url;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createOrder(int amount, String orderInfo, String ipAddress) throws UnsupportedEncodingException {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = orderInfo;
        String orderType = "other";

        String vnp_TxnRef = String.valueOf(System.currentTimeMillis()); // mã giao dịch
        String vnp_IpAddr = ipAddress;

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu *100
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Tạo query string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                 //hashData
                hashData.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()))
                        .append('&');

                // query (PHẢI encode)
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()))
                        .append('&');
//                query.append(fieldName).append('=').append(fieldValue).append('&');
            }
        }

        // Xóa ký tự & cuối
        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);

        // Sinh chữ ký
        String vnp_SecureHash = hmacSHA512(secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        return vnp_Url + "?" + query.toString();
    }

    /**
     * Lấy toàn bộ tham số trả về từ VNPay
     */
    public Map<String, String> extractVnpParams(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            String value = (values != null && values.length > 0) ? values[0] : "";
            fields.put(key, value);
        }
        return fields;
    }

}
