package com.example.shop_management;

import com.example.shop_management.Enum.PaymentMethod;
import com.example.shop_management.Enum.PaymentStatus;
import com.example.shop_management.model.*;
import com.example.shop_management.repository.*;
import com.example.shop_management.service.SpayLaterService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SpayLaterServiceTest {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private OrderHistoryRepository orderHistoryRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private InstallmentRepository installmentRepository;
	@Autowired
	private SpayLaterService spayLaterService;

	private User testUser;

	@BeforeEach
	void setUp() {
		// Tạo user test
		testUser = new User();
		testUser.setUsername("testuser");
		testUser.setPassword("123456");
		testUser.setCredit_limit(BigDecimal.valueOf(1000000));
		testUser = userRepository.save(testUser);
	}

	@Test
	void testCreateInstallmentsAndApplyLateFee() {
		// Tạo order history
		OrderHistory order = new OrderHistory();
		order.setUser(testUser);
		order.setTotal_amount(BigDecimal.valueOf(900000));
		order.setStatus(0);
		order = orderHistoryRepository.save(order);

		// Tạo payment
		Payment payment = new Payment();
		payment.setOrderhistory(order);
		payment.setStatus(PaymentStatus.fromCode(0));
		payment.setPayment_method(PaymentMethod.SPAY_LATER);
		payment = paymentRepository.save(payment);

		// Tạo 3 kỳ trả góp
		spayLaterService.createInstallments(payment, 3);

		List<Installment> installments = installmentRepository.findByPaymentId(payment.getId());
		assertEquals(3, installments.size(), "Phải có 3 kỳ trả góp");

		// Đặt kỳ 1 và 2 quá hạn
		Installment first = installments.get(0);
		first.setDue_date(LocalDateTime.now().minusDays(5));
		Installment second = installments.get(1);
		second.setDue_date(LocalDateTime.now().minusDays(2));
		installmentRepository.save(first);
		installmentRepository.save(second);

		// Gọi applyLateFee
		spayLaterService.applyLateFeeForOverdueInstallments();

		// Reload và kiểm tra
		installments = installmentRepository.findByPaymentId(payment.getId());
		assertTrue(installments.get(1).getLate_fee().compareTo(BigDecimal.ZERO) > 0, "Kỳ 2 phải bị cộng phí trễ hạn");
		assertEquals(BigDecimal.ZERO, installments.get(2).getLate_fee(), "Kỳ 3 chưa đến hạn, không có phí trễ hạn");
	}
}

