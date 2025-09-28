package com.example.shop_management.service;

import com.example.shop_management.Enum.PaymentStatus;
import com.example.shop_management.model.Installment;
import com.example.shop_management.model.Payment;
import com.example.shop_management.repository.InstallmentRepository;
import com.example.shop_management.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpayLaterService {
    private final InstallmentRepository installmentRepo;
    private final PaymentRepository paymentRepo;

    private final double CONVERSION_FEE_RATE = 0.0295; // 2.95%
    private final double LATE_FEE = 30000; // 30k/kỳ trễ hạn

    /**
     * Tạo các kỳ trả góp khi user chọn SPayLater
     */
    public void createInstallments(Payment payment, int installmentCount) {
        BigDecimal totalAmount = payment.getOrderhistory().getTotal_amount();

        // Phí chuyển đổi 2.95% tính trên tổng đơn
        BigDecimal conversionFee = totalAmount.multiply(BigDecimal.valueOf(CONVERSION_FEE_RATE));

        // Số tiền chia đều theo kỳ
        BigDecimal baseInstallment = totalAmount.divide(
                BigDecimal.valueOf(installmentCount),
                0,
                RoundingMode.DOWN
        );

        // Phần dư để dồn vào kỳ cuối
        BigDecimal accumulated = baseInstallment.multiply(BigDecimal.valueOf(installmentCount));
        BigDecimal remainder = totalAmount.subtract(accumulated);

        // Chia đều phí chuyển đổi vào từng kỳ
        BigDecimal feePerInstallment = conversionFee.divide(
                BigDecimal.valueOf(installmentCount),
                0,
                RoundingMode.HALF_UP
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.getDayOfMonth() >= 24
                ? LocalDateTime.of(now.plusMonths(2).getYear(), now.plusMonths(2).getMonth(), 10, 23, 59)
                : LocalDateTime.of(now.plusMonths(1).getYear(), now.plusMonths(1).getMonth(), 10, 23, 59);

        for (int i = 1; i <= installmentCount; i++) {
            Installment installment = new Installment();
            installment.setPayment(payment);
            installment.setInstallment_no((long) i);

            BigDecimal amount = baseInstallment.add(feePerInstallment);

            // Nếu là kỳ cuối thì cộng remainder
            if (i == installmentCount && remainder.compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.add(remainder);
            }

            installment.setAmount(amount);
            installment.setLate_fee(BigDecimal.ZERO); // chưa tính phí trễ
            installment.setDue_date(dueDate);

            installmentRepo.save(installment);
            dueDate = dueDate.plusMonths(1);
        }

        payment.setStatus(PaymentStatus.fromCode(0)); // PENDING
        paymentRepo.save(payment);
    }

    /**
     * Áp dụng phí trễ hạn: ghi nhận 30k vào kỳ tiếp theo
     */
    public void applyLateFeeForOverdueInstallments() {
        LocalDateTime today = LocalDateTime.now();
        List<Installment> installments = installmentRepo.findAll();

        for (int i = 0; i < installments.size(); i++) {
            Installment current = installments.get(i);

            if (current.getDue_date().isBefore(today) && !current.isPaid()) {
                // Nếu chưa trả, cộng phí trễ hạn vào kỳ tiếp theo (nếu có)
                if (i + 1 < installments.size()) {
                    Installment next = installments.get(i + 1);
                    next.setLate_fee(next.getLate_fee().add(BigDecimal.valueOf(LATE_FEE)));
                    installmentRepo.save(next);
                }
            }
        }
    }
}
