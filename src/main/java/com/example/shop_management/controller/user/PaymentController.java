package com.example.shop_management.controller.user;

import com.example.shop_management.DTO.CurrentBillSummary;
import com.example.shop_management.Enum.PaymentMethod;
import com.example.shop_management.Enum.PaymentStatus;
import com.example.shop_management.model.*;
import com.example.shop_management.repository.*;
import com.example.shop_management.service.EmailService;
import com.example.shop_management.service.SpayLaterService;
import com.example.shop_management.service.VNPayService;
import com.example.shop_management.service.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;

    @Autowired
    private EmailService emailService;

    //Form thanh toan COD
    @GetMapping("/checkout")
    public String checkout(Model model,
                           @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tính tổng từ giỏ hàng
        BigDecimal total = BigDecimal.ZERO;
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
            BigDecimal t = items.stream()
                    .map(ci -> BigDecimal.valueOf(ci.getProduct().getItem_price())
                            .multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("cartItems", items);
            model.addAttribute("totalBigDecimal", t);
            model.addAttribute("total", t.intValue());
        });

        model.addAttribute("users", user);
        model.addAttribute("address", new Address());
        return "user/orderaddress";
    }

    //Xu ly submit COD
    @PostMapping("/cod")
    public String submitCOD(
            @RequestParam String province_name,
            @RequestParam String district_name,
            @RequestParam String ward_name,
            @RequestParam String addressLine,
            @RequestParam String postalCode,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {

        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tạo địa chỉ giao hàng
        Address address = new Address();
        address.setUser(user);
        address.setAddressLine(addressLine);
        address.setPostalCode(postalCode);
        address.setProvince_name(province_name);
        address.setDistrict_name(district_name);
        address.setWard_name(ward_name);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        addressRepository.save(address);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        // Kiểm tra tồn kho
        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (item.getQuantity() > product.getStock_quantity()) {
                redirectAttributes.addFlashAttribute("error", "Not enough stock for " + product.getItem_name());
                return "redirect:/user/cart";
            }
        }

        // Trừ hàng
        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStock_quantity(product.getStock_quantity() - item.getQuantity());
            productRepository.save(product);
        }
        productRepository.flush();

        String orderItems = cartItems.stream()
                .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                .collect(Collectors.joining(", "));

        BigDecimal totalAmount = cartItems.stream()
                .map(ci -> BigDecimal.valueOf(ci.getProduct().getItem_price())
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderHistory order = new OrderHistory();
        order.setUser(user);
        order.setAddress(address);
        order.setOrder_items(orderItems);
        order.setTotal_amount(totalAmount);
        order.setStatus(0);
        order.setShipping_status(0);
        order.setCreated_at(LocalDateTime.now());
        order.setUpdated_at(LocalDateTime.now());
        orderHistoryRepository.save(order);

        Payment payment = new Payment();
        payment.setOrderhistory(order);
        payment.setPayment_method(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreated_at(LocalDateTime.now());
        payment.setPaid_at(LocalDateTime.now());
        paymentRepository.save(payment);

        try {
            emailService.sendOrderSuccessEmailViaCOD(user.getEmail(), order);
        } catch (Exception ignored) {}

        cartItemRepository.deleteAll(cartItems);
        redirectAttributes.addFlashAttribute("success", "Ordered successfully");

        return "redirect:/user/home";
    }

    //Thanh toan
    @GetMapping("/pay")
    public String pay(HttpServletRequest request,
                      HttpSession session,
                      @RequestParam("total") int total,
                      @RequestParam("province_name") String province_name,
                      @RequestParam("district_name") String district_name,
                      @RequestParam("ward_name") String ward_name,
                      @RequestParam("addressLine") String addressLine,
                      @RequestParam("postalCode") String postalCode) throws UnsupportedEncodingException {

        Map<String, String> addressInfo = new HashMap<>();
        addressInfo.put("province_name", province_name);
        addressInfo.put("district_name", district_name);
        addressInfo.put("ward_name", ward_name);
        addressInfo.put("addressLine", addressLine);
        addressInfo.put("postalCode", postalCode);
        session.setAttribute("addressInfo", addressInfo);

        String ipAddress = request.getRemoteAddr();
        String paymentUrl = vnPayService.createOrder(total, "Thanh toán đơn hàng", ipAddress);
        return "redirect:" + paymentUrl;
    }


    //Thuc hien thanh toan
    @GetMapping("/payment-return")
    @Transactional
    public String paymentResult(HttpSession session,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal)
            throws UnsupportedEncodingException {

        Map<String, String> vnpParams = vnPayService.extractVnpParams(request);
        String responseCode = vnpParams.get("vnp_ResponseCode");

        if (!VNPayUtils.validateSignature(vnpParams, vnPayService.getSecretKey())) {
            redirectAttributes.addFlashAttribute("error", "Invalid signature from VNPay!");
            return "redirect:/user/home";
        }

        if (!"00".equals(responseCode)) {
            redirectAttributes.addFlashAttribute("error", "Payment failed, code: " + responseCode);
            return "redirect:/user/home";
        }

        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        session.setAttribute("user", user);

        BigDecimal total = new BigDecimal(vnpParams.get("vnp_Amount")).divide(BigDecimal.valueOf(100));
        String orderInfo = vnpParams.get("vnp_OrderInfo");

        // --------- Trả góp: Thanh toán 1 kỳ hoặc tất cả ----------
        if (orderInfo != null && orderInfo.startsWith("installment:")) {
            Long installmentNo = Long.parseLong(orderInfo.split(":")[1]);

            List<Installment> installments = installmentRepository.findUnpaidByInstallmentNoAndUser(installmentNo, user.getId());
            if (installments.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No unpaid installments found for this batch!");
                return "redirect:/user/spay-later";
            }

            String paymentBatchId = UUID.randomUUID().toString();

            for (Installment installment : installments) {
                installment.setInstallment_batch_id(paymentBatchId);
                installment.setPaid(true);
                installment.setPaid_at(LocalDateTime.now());

                // Giu nguyen paid_fee đã có từ DB
                // Chỉ cộng thêm late_fee nếu có
                BigDecimal existingPaidFee = Optional.ofNullable(installment.getPaid_fee()).orElse(BigDecimal.ZERO);
                BigDecimal lateFee = Optional.ofNullable(installment.getLate_fee()).orElse(BigDecimal.ZERO);

                // Nếu có late_fee, cộng thêm vào paid_fee
                if (lateFee.compareTo(BigDecimal.ZERO) > 0) {
                    installment.setPaid_fee(existingPaidFee.add(lateFee));
                }

                installmentRepository.save(installment);

                try {
                    User insUser = installments.get(0).getPayment().getOrderhistory().getUser();
                    emailService.sendPaymentSpayLaterSuccessEmail(insUser.getEmail(), installment, principal);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            BigDecimal totalAmount = installments.stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            User insUser = installments.get(0).getPayment().getOrderhistory().getUser();
            insUser.setCredit_limit(insUser.getCredit_limit().add(totalAmount));

            BigDecimal unpaidTotal = installmentRepository.findByUserId(insUser.getId()).stream()
                    .filter(i -> !i.isPaid())
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            session.setAttribute("currentPaymentPeriod", unpaidTotal);
            userRepository.save(insUser);
            session.setAttribute("user", insUser);

            redirectAttributes.addFlashAttribute("success", "Installment batch paid successfully!");
            return "redirect:/user/spay-later";

        } else if (orderInfo != null && orderInfo.startsWith("all_unpaid_installments")) {
            Long userId = Long.parseLong(orderInfo.split("=")[1]);
            User insUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Installment> unpaid = installmentRepository.findByUserId(userId).stream()
                    .filter(i -> !i.isPaid())
                    .toList();

            unpaid.forEach(i -> {
                i.setPaid(true);
                i.setPaid_at(LocalDateTime.now());

                // Tinh phi rieng: 2.95% × amount
                BigDecimal fee = i.getAmount().multiply(BigDecimal.valueOf(0.0295))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal late = Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO);
                i.setPaid_fee(fee.add(late));
            });
            installmentRepository.saveAll(unpaid);

            insUser.setCredit_limit(insUser.getCredit_limit().add(total));
            userRepository.save(insUser);
            session.setAttribute("user", insUser);

            redirectAttributes.addFlashAttribute("success", "All installments paid successfully!");
            return "redirect:/user/spay-later";
        }

        // --------- Thanh toán đơn hàng bình thường ----------
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (item.getQuantity() > product.getStock_quantity()) {
                redirectAttributes.addFlashAttribute("error", "Not enough stock for " + product.getItem_name());
                return "redirect:/user/cart";
            }
        }

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStock_quantity(product.getStock_quantity() - item.getQuantity());
            productRepository.save(product);
        }
        productRepository.flush();

        Map<String, String> addressInfo = (Map<String, String>) session.getAttribute("addressInfo");
        if (addressInfo == null) {
            redirectAttributes.addFlashAttribute("error", "Missing address information!");
            return "redirect:/user/cart";
        }

        Address address = new Address();
        address.setUser(user);
        address.setAddressLine(addressInfo.get("addressLine"));
        address.setPostalCode(addressInfo.get("postalCode"));
        address.setProvince_name(addressInfo.get("province_name"));
        address.setDistrict_name(addressInfo.get("district_name"));
        address.setWard_name(addressInfo.get("ward_name"));
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        addressRepository.save(address);

        String orderItems = cartItems.stream()
                .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                .collect(Collectors.joining(", "));

        OrderHistory order = new OrderHistory();
        order.setUser(user);
        order.setAddress(address);
        order.setTotal_amount(total);
        order.setOrder_items(orderItems);
        order.setShipping_status(0);
        order.setStatus(1);
        order.setCreated_at(LocalDateTime.now());
        order.setUpdated_at(LocalDateTime.now());
        order = orderHistoryRepository.save(order);

        Payment payment = new Payment();
        payment.setOrderhistory(order);
        payment.setPayment_method(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreated_at(LocalDateTime.now());
        payment.setPaid_at(LocalDateTime.now());
        paymentRepository.save(payment);

        try {
            emailService.sendOrderSuccessEmail(user.getEmail(), order);
        } catch (Exception ignored) {}

        cartItemRepository.deleteAll(cartItems);
        session.removeAttribute("addressInfo");

        redirectAttributes.addFlashAttribute("success", "Payment successful via VNPay!");
        return "redirect:/user/home";
    }

    @GetMapping("/spay-later")
    public String viewSpayLater(Model model,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {

        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Installment> installments = installmentRepository.findByUserId(user.getId());

        List<Installment> unpaidBills = installments.stream()
                .filter(i -> !i.isPaid())
                .sorted(Comparator.comparing(Installment::getDue_date))
                .collect(Collectors.toList());

        List<Installment> paidBills = installments.stream()
                .filter(Installment::isPaid)
                .sorted(Comparator.comparing(Installment::getPaid_at).reversed())
                .collect(Collectors.toList());

        // Tổng tiền gốc của TẤT CẢ kỳ chưa trả
        BigDecimal totalPrincipal = unpaidBills.stream()
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //PHÍ CHUYỂN ĐỔI CHUNG cho hiển thị tổng quan: 2.95% × TỔNG principal
        BigDecimal totalConversionFee = totalPrincipal.multiply(BigDecimal.valueOf(0.0295))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalLateFee = unpaidBills.stream()
                .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LinkedHashMap<Long, List<Installment>> groupedUnpaidBills = unpaidBills.stream()
                .collect(Collectors.groupingBy(
                        Installment::getInstallment_no,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Long, BigDecimal> principalByGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> feeByGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> lateFeeByGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> grandTotalByGroup = new LinkedHashMap<>();

        for (Map.Entry<Long, List<Installment>> entry : groupedUnpaidBills.entrySet()) {
            // Tính tổng tiền gốc của nhóm này
            BigDecimal principalOfGroup = entry.getValue().stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // CỘNG TỔNG paid_fee TỪ DB cho từng installment trong nhóm
            BigDecimal feeOfGroup = entry.getValue().stream()
                    .map(i -> Optional.ofNullable(i.getPaid_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Phí trễ hạn của nhóm này
            BigDecimal lateFeeOfGroup = entry.getValue().stream()
                    .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // TỔNG TIỀN NHÓM = gốc_nhóm + paid_fee_nhóm + phí_trễ_nhóm
            BigDecimal totalOfGroup = principalOfGroup.add(feeOfGroup).add(lateFeeOfGroup);

            principalByGroup.put(entry.getKey(), principalOfGroup);
            feeByGroup.put(entry.getKey(), feeOfGroup);
            lateFeeByGroup.put(entry.getKey(), lateFeeOfGroup);
            grandTotalByGroup.put(entry.getKey(), totalOfGroup);
        }


        // GROUP PAID BILLS THEO payment_batch_id THAY VÌ installment_no
        LinkedHashMap<String, List<Installment>> groupedPaidBills = paidBills.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getInstallment_batch_id() != null ? i.getInstallment_batch_id() : "unknown",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, BigDecimal> principalByPaidGroup = new LinkedHashMap<>();
        Map<String, BigDecimal> feeByPaidGroup = new LinkedHashMap<>();
        Map<String, BigDecimal> lateFeeByPaidGroup = new LinkedHashMap<>();
        Map<String, BigDecimal> grandTotalByPaidGroup = new LinkedHashMap<>();
        Map<String, LocalDateTime> paidDateByBatch = new LinkedHashMap<>();

        // TÍNH TOÁN THEO payment_batch_id
        for (Map.Entry<String, List<Installment>> entry : groupedPaidBills.entrySet()) {
            String batchId = entry.getKey();

            // Tiền gốc
            BigDecimal principalOfPaidGroup = entry.getValue().stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // PHÍ ĐÃ TRẢ - Lấy từ paid_fee trong DB
            BigDecimal paidFeeOfGroup = entry.getValue().stream()
                    .map(i -> Optional.ofNullable(i.getPaid_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // PHÍ TRỄ HẠN
            BigDecimal lateFeeOfPaidGroup = entry.getValue().stream()
                    .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // TỔNG
            BigDecimal totalOfPaidGroup = principalOfPaidGroup.add(paidFeeOfGroup).add(lateFeeOfPaidGroup);

            // LẤY NGÀY TRẢ TỪ INSTALLMENT ĐẦU TIÊN CỦA BATCH
            LocalDateTime paidDate = entry.getValue().get(0).getPaid_at();

            principalByPaidGroup.put(batchId, principalOfPaidGroup);
            feeByPaidGroup.put(batchId, paidFeeOfGroup);
            lateFeeByPaidGroup.put(batchId, lateFeeOfPaidGroup);
            grandTotalByPaidGroup.put(batchId, totalOfPaidGroup);
            paidDateByBatch.put(batchId, paidDate);
        }

        List<Installment> nextUnpaidGroup = Collections.emptyList();
        if (!unpaidBills.isEmpty()) {
            Installment nextUnpaid = unpaidBills.get(0);
            Long nextInstallmentNo = nextUnpaid.getInstallment_no();
            nextUnpaidGroup = groupedUnpaidBills.getOrDefault(nextInstallmentNo, Collections.emptyList());
        }

        model.addAttribute("user", user);
        model.addAttribute("availableBalance", user.getCredit_limit());
        model.addAttribute("unpaidBills", unpaidBills);
        model.addAttribute("paidBills", paidBills);
        model.addAttribute("groupedUnpaidBills", groupedUnpaidBills);
        model.addAttribute("principalByGroup", principalByGroup);
        model.addAttribute("feeByGroup", feeByGroup);
        model.addAttribute("lateFeeByGroup", lateFeeByGroup);
        model.addAttribute("grandTotalByGroup", grandTotalByGroup);
        model.addAttribute("nextUnpaidGroup", nextUnpaidGroup);
        model.addAttribute("hasNextUnpaid", !nextUnpaidGroup.isEmpty());
        model.addAttribute("groupedPaidBills", groupedPaidBills);
        model.addAttribute("principalByPaidGroup", principalByPaidGroup);
        model.addAttribute("feeByPaidGroup", feeByPaidGroup);
        model.addAttribute("lateFeeByPaidGroup", lateFeeByPaidGroup);
        model.addAttribute("grandTotalByPaidGroup", grandTotalByPaidGroup);
        model.addAttribute("currentBills",
                new CurrentBillSummary(totalPrincipal, totalConversionFee, totalLateFee, unpaidBills));

        return "user/SpayLater";
    }

    @GetMapping("/spay-later/checkout")
    @Transactional
    public String paymentSpayLater(@RequestParam("period") int period,
                                   @RequestParam String province_name,
                                   @RequestParam String district_name,
                                   @RequestParam String ward_name,
                                   @RequestParam String addressLine,
                                   @RequestParam String postalCode,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) throw new RuntimeException("User not logged in");

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (item.getQuantity() > product.getStock_quantity()) {
                redirectAttributes.addFlashAttribute("error", "Not enough stock for " + product.getItem_name());
                return "redirect:/user/cart";
            }
        }

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStock_quantity(product.getStock_quantity() - item.getQuantity());
            productRepository.save(product);
        }
        productRepository.flush();

        String orderItems = cartItems.stream()
                .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                .collect(Collectors.joining(", "));

        BigDecimal orderAmount = cartItems.stream()
                .map(ci -> BigDecimal.valueOf(ci.getProduct().getItem_price())
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (user.getCredit_limit().compareTo(orderAmount) < 0) {
            redirectAttributes.addFlashAttribute("error", "Insufficient credit limit");
            return "redirect:/user/cart";
        }

        user.setCredit_limit(user.getCredit_limit().subtract(orderAmount));
        userRepository.save(user);

        Address address = new Address();
        address.setUser(user);
        address.setAddressLine(addressLine);
        address.setPostalCode(postalCode);
        address.setProvince_name(province_name);
        address.setDistrict_name(district_name);
        address.setWard_name(ward_name);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        addressRepository.save(address);

        OrderHistory order = new OrderHistory();
        order.setUser(user);
        order.setAddress(address);
        order.setTotal_amount(orderAmount);
        order.setShipping_status(0);
        order.setOrder_items(orderItems);
        order.setStatus(0);
        order.setCreated_at(LocalDateTime.now());
        order.setUpdated_at(LocalDateTime.now());
        order = orderHistoryRepository.save(order);

        Payment payment = new Payment();
        payment.setOrderhistory(order);
        payment.setStatus(PaymentStatus.fromCode(0));
        payment.setPayment_method(PaymentMethod.SPAY_LATER);
        payment = paymentRepository.save(payment);

        spayLaterService.createInstallments(payment, period);
        emailService.sendOrderSuccessEmailViaSpayLater(user.getEmail(), order);

        cartItemRepository.deleteAll(cartItems);

        redirectAttributes.addFlashAttribute("success", "Checkout with SpayLater successful!");
        return "redirect:/user/spay-later";
    }

    @GetMapping("/bills/pay/{no}")
    public String payFullInstallment(@PathVariable("no") Long installmentNo,
                                     HttpServletRequest request,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) throws UnsupportedEncodingException {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("User not logged in");
        }

        List<Installment> allUnpaidInstallments = installmentRepository.findAllUnpaidByUser(user.getId());
        if (allUnpaidInstallments.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không có kỳ trả góp nào chưa thanh toán.");
            return "redirect:/user/spay-later";
        }

        List<Installment> unpaidBills = allUnpaidInstallments.stream()
                .filter(i -> !i.isPaid())
                .sorted(Comparator.comparing(Installment::getDue_date))
                .collect(Collectors.toList());

        List<Installment> paidBills = allUnpaidInstallments.stream()
                .filter(Installment::isPaid)
                .sorted(Comparator.comparing(Installment::getPaid_at).reversed())
                .collect(Collectors.toList());

        List<Installment> currentInstallments =
                installmentRepository.findUnpaidByInstallmentNoAndUser(installmentNo, user.getId());
        if (currentInstallments.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không có khoản nào chưa thanh toán trong đợt này.");
            return "redirect:/user/spay-later";
        }

        // Tính tổng tiền gốc của kỳ hiện tại
        BigDecimal principal = currentInstallments.stream()
                .map(i -> Optional.ofNullable(i.getAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // CỘNG TỔNG paid_fee từ DB của các installment trong kỳ này
        BigDecimal conversionFee = currentInstallments.stream()
                .map(i -> Optional.ofNullable(i.getPaid_fee()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính phí trễ hạn (nếu có)
        BigDecimal lateFee = currentInstallments.stream()
                .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tổng thanh toán: gốc + paid_fee_từ_DB + trễ hạn
        BigDecimal totalAmount = principal.add(conversionFee).add(lateFee)
                .setScale(0, RoundingMode.HALF_UP);

        String ip = request.getRemoteAddr();
        String orderInfo = "installment:" + installmentNo;
        String url = vnPayService.createOrder(totalAmount.intValue(), orderInfo, ip);

        return "redirect:" + url;
    }
}