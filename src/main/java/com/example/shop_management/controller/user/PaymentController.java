package com.example.shop_management.controller.user;

import com.example.shop_management.DTO.CurrentBillSummary;
import com.example.shop_management.Enum.PaymentMethod;
import com.example.shop_management.Enum.PaymentStatus;
import com.example.shop_management.model.*;
import com.example.shop_management.repository.*;
import com.example.shop_management.service.SpayLaterService;
import com.example.shop_management.service.VNPayService;
import com.example.shop_management.service.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final InstallmentRepository installmentRepository;
    private final SpayLaterService spayLaterService;
    private final PaymentRepository paymentRepository;


    //Gọi sang VNPay tạo đơn hàng
    @GetMapping("/pay")
    public String pay(HttpServletRequest request,
                      @RequestParam("amount") int amount) throws UnsupportedEncodingException {
        String ipAddress = request.getRemoteAddr();
        String paymentUrl = vnPayService.createOrder(
                amount,
                "Thanh toán đơn hàng",
                ipAddress
        );
        return "redirect:" + paymentUrl;
    }


    //Thanh toan va tra lai trang thai sau khi thanh toan bang Vnpay
    @GetMapping("/payment-return")
    @Transactional
    public String paymentResult(HttpSession session, HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) throws UnsupportedEncodingException {

        // Lấy toàn bộ params trả về từ VNPay
        Map<String, String> vnpParams = vnPayService.extractVnpParams(request);
        String responseCode = vnpParams.get("vnp_ResponseCode");

        // Kiểm tra chữ ký
        if (VNPayUtils.validateSignature(vnpParams, vnPayService.getSecretKey())) {
            if ("00".equals(responseCode)) { // Thanh toán thành công
                String username = principal.getUsername();
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // Lấy số tiền thanh toán
                String amountStr = vnpParams.get("vnp_Amount");
                if (amountStr == null) {
                    redirectAttributes.addFlashAttribute("error", "Thiếu tham số số tiền!");
                    return "redirect:/user/home";
                }
                BigDecimal amount = BigDecimal.valueOf(Long.parseLong(amountStr) / 100);

                // Phân biệt loại thanh toán qua vnp_OrderInfo
                String orderInfo = vnpParams.get("vnp_OrderInfo");

                if (orderInfo != null && orderInfo.startsWith("installment:")) {
                    // ---- Thanh toán 1 kỳ ----
                    Long installmentId = Long.parseLong(orderInfo.split(":")[1]);

                    Installment ins = installmentRepository.findById(installmentId)
                            .orElseThrow(() -> new RuntimeException("Installment not found"));

                    if (!ins.isPaid()) {
                        ins.setPaid(true);
                        ins.setPaid_at(LocalDateTime.now()); // ✅ set ngày trả
                        installmentRepository.save(ins);

                        // Hồi lại hạn mức
                        User insUser = ins.getPayment().getOrderhistory().getUser();
                        insUser.setCredit_limit(insUser.getCredit_limit().add(amount));
                        userRepository.save(insUser);

                        session.setAttribute("user", insUser);
                    }

                    redirectAttributes.addFlashAttribute("success", "Installment payment successfully");

                } else if (orderInfo != null && orderInfo.startsWith("all_unpaid_installments")) {
                    // ---- Thanh toán toàn bộ ----
                    Long userId = Long.parseLong(orderInfo.split("=")[1]);
                    User insUser = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    // Lấy tất cả installments chưa trả
                    List<Installment> unpaidList = installmentRepository.findByUserId(userId).stream()
                            .filter(i -> !i.isPaid())
                            .toList();

                    // ✅ Đánh dấu tất cả là paid, không xóa
                    LocalDateTime now = LocalDateTime.now();
                    for (Installment i : unpaidList) {
                        i.setPaid(true);
                        i.setPaid_at(now);
                    }
                    installmentRepository.saveAll(unpaidList);

                    // Cộng lại credit_limit
                    insUser.setCredit_limit(insUser.getCredit_limit().add(amount));
                    userRepository.save(insUser);

                    // Cập nhật session
                    session.setAttribute("user", insUser);

                    redirectAttributes.addFlashAttribute("success", "All unpaid installments marked as paid and credit updated");
                } else {
                    // ---- Thanh toán đơn hàng bình thường ----
                    Cart cart = cartRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new RuntimeException("Cart not found"));

                    List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

                    // Ghép chuỗi order_items
                    String orderItems = cartItems.stream()
                            .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                            .collect(Collectors.joining(", "));

                    // Tạo OrderHistory
                    OrderHistory orderHistory = new OrderHistory();
                    orderHistory.setUser(user);
                    orderHistory.setOrder_items(orderItems);
                    orderHistory.setStatus(1); // 1 = Đã thanh toán
                    orderHistory.setTotal_amount(amount);
                    orderHistory.setCreated_at(LocalDateTime.now());
                    orderHistory.setUpdated_at(LocalDateTime.now());

                    orderHistoryRepository.save(orderHistory);

                    // Xóa giỏ hàng
                    cartItemRepository.deleteAll(cartItems);

                    redirectAttributes.addFlashAttribute("success", "Paid successfully");
                }

            } else {
                redirectAttributes.addFlashAttribute("error", "Paid unsuccessfully, error code: " + responseCode);
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "The sign is not valid!");
        }

        return "redirect:/user/home";
    }


    @GetMapping("/spay-later")
    public String viewSpayLater(HttpSession session, Model model, Principal principal) {
        User user = (User) session.getAttribute("user");
        if (user == null) throw new RuntimeException("User not logged in");

        List<Installment> installments = installmentRepository.findByUserId(user.getId());

        // Danh sách chưa trả (sort theo ngày đến hạn)
        List<Installment> unpaidBills = installments.stream()
                .filter(i -> !i.isPaid())
                .sorted(Comparator.comparing(Installment::getDue_date))
                .collect(Collectors.toList());

        // Danh sách đã trả (sort theo ngày trả mới nhất)
        List<Installment> paidBills = installments.stream()
                .filter(Installment::isPaid)
                .sorted(Comparator.comparing(Installment::getPaid_at).reversed())
                .collect(Collectors.toList());

        // Kỳ gần nhất chưa trả
        Installment nextUnpaid = unpaidBills.isEmpty() ? null : unpaidBills.get(0);

        // Tính toán tổng tiền, phí, late fee
        BigDecimal totalAmount = unpaidBills.stream()
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLateFee = unpaidBills.stream()
                .map(i -> i.getLate_fee() == null ? BigDecimal.ZERO : i.getLate_fee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFee = totalAmount.multiply(BigDecimal.valueOf(0.02));
        CurrentBillSummary currentBills = new CurrentBillSummary(totalAmount, totalFee, totalLateFee, unpaidBills);

        model.addAttribute("user", user);
        model.addAttribute("availableBalance", user.getCredit_limit());
        model.addAttribute("unpaidBills", unpaidBills); // vẫn giữ để hiển thị danh sách nếu cần
        model.addAttribute("paidBills", paidBills);
        model.addAttribute("nextUnpaid", nextUnpaid);   // 👉 dùng cái này để hiển thị 1 kỳ duy nhất
        model.addAttribute("currentBills", currentBills);

        String username = principal.getName();
        User users = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        model.addAttribute("users", users);

        return "user/SpayLater";
    }



    // Endpoint checkout trả góp
    @GetMapping("/spay-later/checkout")
    public String paymentSpayLater(@RequestParam("period") int period,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) throw new RuntimeException("User not logged in");

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getCartItem().isEmpty()) {
            throw new RuntimeException("Cart is empty, cannot create order");
        }

        // Ghép items
        String orderItems = cart.getCartItem().stream()
                .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                .collect(Collectors.joining(", "));

        // Tổng tiền
        BigDecimal orderAmount = cart.getCartItem().stream()
                .map(ci -> BigDecimal.valueOf(ci.getProduct().getItem_price())
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (user.getCredit_limit().compareTo(orderAmount) < 0) {
            throw new RuntimeException("Insufficient credit limit");
        }
        user.setCredit_limit(user.getCredit_limit().subtract(orderAmount));
        userRepository.save(user);

        // Tạo order
        OrderHistory order = new OrderHistory();
        order.setUser(user);
        order.setTotal_amount(orderAmount);
        order.setStatus(1);
        order.setOrder_items(orderItems);
        order = orderHistoryRepository.save(order);

        // Tạo payment
        Payment payment = new Payment();
        payment.setOrderhistory(order);
        payment.setStatus(PaymentStatus.fromCode(0));
        payment.setPayment_method(PaymentMethod.SPAY_LATER);
        payment = paymentRepository.save(payment);

        // Tạo installments theo period (có fee 2.95% và late fee xử lý trong service)
        spayLaterService.createInstallments(payment, period);

        // Clear giỏ hàng
        cart.getCartItem().clear();
        cartRepository.save(cart);

        redirectAttributes.addFlashAttribute("success", "Checkout successfully!");
        return "redirect:/user/spay-later";
    }

    // Thanh toán tổng tất cả số tiền (unpaid + late fee)
    @PostMapping("/bills/pay-current")
    public ResponseEntity<String> payCurrentBill(HttpSession session,
                                                 HttpServletRequest request) throws UnsupportedEncodingException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("User not logged in");
        }

        // Lấy tổng số tiền chưa trả (bao gồm phí trễ hạn)
        BigDecimal totalUnpaid = installmentRepository.findByUserId(user.getId()).stream()
                .filter(i -> !i.isPaid())
                .map(i -> i.getAmount().add(i.getLate_fee() == null ? BigDecimal.ZERO : i.getLate_fee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalUnpaid.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("No unpaid bills");
        }

        String orderInfo = "all_unpaid_installments:userId=" + user.getId();
        String ipAddress = request.getRemoteAddr();

        String paymentUrl = vnPayService.createOrder(totalUnpaid.intValue(), orderInfo, ipAddress);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, paymentUrl)
                .build();
    }



}
