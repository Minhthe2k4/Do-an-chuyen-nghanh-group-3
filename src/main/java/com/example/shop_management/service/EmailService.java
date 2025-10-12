package com.example.shop_management.service;

import com.example.shop_management.model.Installment;
import com.example.shop_management.model.OrderHistory;
import com.example.shop_management.model.User;
import com.example.shop_management.repository.InstallmentRepository;
import com.example.shop_management.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    public void sendOrderSuccessEmail(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng #" + orderHistory.getId());

            StringBuilder content = new StringBuilder();
            content.append("Xin chào ").append(orderHistory.getUser().getEmail()).append(",<br><br>")
                    .append("Cảm ơn bạn đã đặt hàng tại <b>Gian hàng thông minh</b>.<br>")
                    .append("Thông tin đơn hàng của bạn như sau:<br><br>")
                    .append("Mã đơn hàng: <b>").append(orderHistory.getId()).append("</b><br>")
                    .append("Ngày đặt: ").append(orderHistory.getCreated_at()).append("<br><br>")
                    .append("Sản phẩm:<br>")
                    .append(orderHistory.getOrder_items()).append("<br><br>")
                    .append("Tổng thanh toán: <b>").append(orderHistory.getTotal_amount()).append(" VND</b><br><br>")
                    .append("Trạng thái: ").append(orderHistory.getStatus() == 1 ? "Đã thanh toán" : "Chưa thanh toán").append("<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            helper.setText(content.toString(), true);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    public void sendOrderSuccessEmailViaCOD(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng #" + orderHistory.getId());

            StringBuilder content = new StringBuilder();
            content.append("Xin chào ").append(orderHistory.getUser().getEmail()).append(",<br><br>")
                    .append("Cảm ơn bạn đã đặt hàng tại <b>Gian hàng thông minh</b>.<br>")
                    .append("Thông tin đơn hàng của bạn như sau:<br><br>")
                    .append("Mã đơn hàng: <b>").append(orderHistory.getId()).append("</b><br>")
                    .append("Ngày đặt: ").append(orderHistory.getCreated_at()).append("<br><br>")
                    .append("Sản phẩm:<br>")
                    .append(orderHistory.getOrder_items()).append("<br><br>")
                    .append("Tổng thanh toán: <b>").append(orderHistory.getTotal_amount()).append(" VND</b><br><br>")
                    .append("Trạng thái: ").append(orderHistory.getStatus() == 1 ? "Đã thanh toán" : "Chưa thanh toán").append("<br><br>")
                    .append("Qúy khách vui lòng theo dõi tình trạng đơn hàng qua website của chúng tôi, sản phẩm sẽ được giao trong vòng từ 3 - 5 ngày!<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            helper.setText(content.toString(), true);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    public void sendOrderSuccessEmailViaSpayLater(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đơn hàng #" + orderHistory.getId());

            StringBuilder content = new StringBuilder();
            content.append("Xin chào ").append(orderHistory.getUser().getEmail()).append(",<br><br>")
                    .append("Cảm ơn bạn đã đặt hàng qua hình thức Spay later tại <b>Gian hàng thông minh</b> của chúng tôi.<br>")
                    .append("Thông tin đơn hàng của bạn như sau:<br><br>")
                    .append("Mã đơn hàng: <b>").append(orderHistory.getId()).append("</b><br>")
                    .append("Ngày đặt: ").append(orderHistory.getCreated_at()).append("<br><br>")
                    .append("Sản phẩm:<br>")
                    .append(orderHistory.getOrder_items()).append("<br><br>")
                    .append("Tổng thanh toán: <b>").append(orderHistory.getTotal_amount()).append(" VND</b><br><br>")
                    .append("Trạng thái: ").append(orderHistory.getStatus() == 1 ? "Đã thanh toán" : "Chưa thanh toán").append("<br><br>")
                    .append("Qúy khách vui lòng theo dõi tình trạng đơn hàng qua website của chúng tôi, sản phẩm sẽ được giao trong vòng từ 3 - 5 ngày!<br><br>")
                    .append("<b>Lưu ý: </b> Qúy khách cần xem thật kỹ các khoản thanh toán tại mục SpayLater tại mục Menu của trang chủ<br>")
                    .append("Nếu quý khách có bất cứ vấn đề nào về dịch vụ, vui lòng liên hệ qua email: gianhangthongminh@gmail.com<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            helper.setText(content.toString(), true);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    public void sendCancelledOrderSuccessEmail(String toEmail, OrderHistory orderHistory) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận hủy đơn hàng #" + orderHistory.getId());

            StringBuilder content = new StringBuilder();
            content.append("Xin chào ").append(orderHistory.getUser().getEmail()).append(",<br><br>")
                    .append("Cảm ơn bạn sử dụng dịch vụ tại <b>Gian hàng thông minh</b> của chúng tôi.<br>")
                    .append("Đơn hàng của bạn đã hủy thành công!<br>")
                    .append("Thông tin đơn hàng mà bạn xác nhận hủy như sau:<br><br>")
                    .append("Mã đơn hàng: <b>").append(orderHistory.getId()).append("</b><br>")
                    .append("Ngày đặt: ").append(orderHistory.getCreated_at()).append("<br><br>")
                    .append("Sản phẩm:<br>")
                    .append(orderHistory.getOrder_items()).append("<br><br>")
                    .append("Tổng thanh toán: <b>").append(orderHistory.getTotal_amount()).append(" VND</b><br><br>")
                    .append("Nếu quý khách có bất cứ vấn đề nào về dịch vụ, vui lòng liên hệ qua email: gianhangthongminh@gmail.com<br><br>")
                    .append("Xin cảm ơn và hẹn gặp lại!<br>");

            helper.setText(content.toString(), true);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    /**
     * Gửi email nhắc nhở thanh toán sắp đến hạn
     * Tính phí dựa trên paid_fee đã lưu trong DB
     */
    public void sendInstallmentReminderEmail(String toEmail, Long installmentNo, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Lấy các installment của đợt này (cùng installment_no)
            List<Installment> currentInstallments = installmentRepository.findByUserId(userId).stream()
                    .filter(i -> !i.isPaid() && i.getInstallment_no().equals(installmentNo))
                    .collect(Collectors.toList());

            if (currentInstallments.isEmpty()) {
                return; // Không có gì để nhắc
            }

            Installment firstInstallment = currentInstallments.get(0);

            // Tính tổng từ DB
            BigDecimal principal = currentInstallments.stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // LẤY paid_fee TỪ DB (không tính lại 2.95%)
            BigDecimal fee = currentInstallments.stream()
                    .map(i -> Optional.ofNullable(i.getPaid_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal lateFee = currentInstallments.stream()
                    .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal total = principal.add(fee).add(lateFee);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");
            helper.setTo(toEmail);
            helper.setSubject("Nhắc nhở thanh toán trả góp sắp đến hạn - Kỳ #" + installmentNo);

            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

            StringBuilder content = new StringBuilder();
            content.append("<p>Xin chào ").append(user.getFull_name()).append(",</p>")
                    .append("<p>Khoản trả góp <b>Kỳ #").append(installmentNo).append("</b> của bạn sắp đến hạn.</p>")
                    .append("<p>Ngày đến hạn: <b>").append(firstInstallment.getDue_date().toLocalDate()).append("</b></p>")
                    .append("<p><b>Chi tiết thanh toán:</b></p><ul>")
                    .append("<li>Tiền gốc: ").append(formatter.format(principal)).append(" VNĐ</li>")
                    .append("<li>Phí chuyển đổi: ").append(formatter.format(fee)).append(" VNĐ</li>");

            if (lateFee.compareTo(BigDecimal.ZERO) > 0) {
                content.append("<li>Phí trễ hạn: ").append(formatter.format(lateFee)).append(" VNĐ</li>");
            }

            content.append("</ul>")
                    .append("<p><b>Tổng cần thanh toán:</b> ").append(formatter.format(total)).append(" VNĐ</p>")
                    .append("<p>Vui lòng thanh toán đúng hạn để tránh phát sinh phí phạt.</p>")
                    .append("<br><p>Trân trọng,<br>Gian hàng thông minh</p>");

            helper.setText(content.toString(), true);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email nhắc nhở", e);
        }
    }

    /**
     * Gửi email xác nhận thanh toán trả góp thành công
     * Lấy paid_fee từ DB thay vì tính lại
     */
    public void sendPaymentSpayLaterSuccessEmail(String toEmail, Installment installment,
                                                 org.springframework.security.core.userdetails.User principal) {
        try {
            String username = principal.getUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // LẤY payment_batch_id thay vì chỉ installment_no
            String paymentBatchId = installment.getInstallment_batch_id();

            // Query theo payment_batch_id để lấy TẤT CẢ khoản trong lần thanh toán này
            List<Installment> paidInstallments = installmentRepository.findByUserId(user.getId()).stream()
                    .filter(i -> i.isPaid() && i.getInstallment_batch_id() != null
                            && i.getInstallment_batch_id().equals(paymentBatchId))
                    .collect(Collectors.toList());

            if (paidInstallments.isEmpty()) {
                return;
            }

            // Tính tổng từ DB
            BigDecimal totalPrincipal = paidInstallments.stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalFee = paidInstallments.stream()
                    .map(i -> Optional.ofNullable(i.getPaid_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalLateFee = paidInstallments.stream()
                    .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal grandTotal = totalPrincipal.add(totalFee).add(totalLateFee);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("gianhangthongminh@gmail.com", "Gian hàng thông minh");
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận thanh toán trả góp thành công - Batch #" + paymentBatchId);

            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

            StringBuilder content = new StringBuilder();
            content.append("<p>Xin chào ").append(user.getFull_name()).append(",</p>")
                    .append("<p>Lần thanh toán của bạn đã được xác nhận thành công!</p>")
                    .append("<p><b>Chi tiết thanh toán:</b></p><table border='1' cellpadding='10' style='border-collapse: collapse; width: 100%;'>")
                    .append("<thead><tr style='background-color: #f2f2f2;'>")
                    .append("<th>Đơn hàng</th>")
                    .append("<th>Kỳ</th>")
                    .append("<th>Tiền gốc</th>")
                    .append("<th>Phí chuyển đổi</th>")
                    .append("<th>Phí trễ hạn</th>")
                    .append("<th>Tổng cộng</th>")
                    .append("</tr></thead><tbody>");

            // Hiển thị RIÊNG từng đơn hàng trong lần thanh toán này
            for (Installment inst : paidInstallments) {
                OrderHistory ord = inst.getPayment().getOrderhistory();
                BigDecimal principal1 = inst.getAmount();
                BigDecimal fee = Optional.ofNullable(inst.getPaid_fee()).orElse(BigDecimal.ZERO);
                BigDecimal lateFee = Optional.ofNullable(inst.getLate_fee()).orElse(BigDecimal.ZERO);
                BigDecimal subTotal = principal1.add(fee).add(lateFee);

                content.append("<tr>")
                        .append("<td>#").append(ord.getId()).append("</td>")
                        .append("<td>#").append(inst.getInstallment_no()).append("</td>")
                        .append("<td>").append(formatter.format(principal1)).append(" VNĐ</td>")
                        .append("<td>").append(formatter.format(fee)).append(" VNĐ</td>")
                        .append("<td>").append(formatter.format(lateFee)).append(" VNĐ</td>")
                        .append("<td><b>").append(formatter.format(subTotal)).append(" VNĐ</b></td>")
                        .append("</tr>");
            }

            content.append("</tbody></table>")
                    .append("<br><p><b>Tổng hợp:</b></p><ul>")
                    .append("<li>Tổng tiền gốc: ").append(formatter.format(totalPrincipal)).append(" VNĐ</li>")
                    .append("<li>Tổng phí chuyển đổi: ").append(formatter.format(totalFee)).append(" VNĐ</li>");

            if (totalLateFee.compareTo(BigDecimal.ZERO) > 0) {
                content.append("<li>Tổng phí trễ hạn: ").append(formatter.format(totalLateFee)).append(" VNĐ</li>");
            }

            content.append("</ul>")
                    .append("<p style='font-size: 16px; color: #d32f2f;'><b>💰 Tổng đã thanh toán: ")
                    .append(formatter.format(grandTotal)).append(" VNĐ</b></p>")
                    .append("<p>Ngày thanh toán: ").append(installment.getPaid_at().toLocalDate()).append("</p><br>")
                    .append("<p>Nếu bạn có thắc mắc, vui lòng liên hệ qua email: <b>gianhangthongminh@gmail.com</b></p>")
                    .append("<br><p>Trân trọng,<br>Gian hàng thông minh</p>");

            helper.setText(content.toString(), true);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email xác nhận trả góp", e);
        }
    }
}