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
    //private final UserRepository userRepository;
   // private final OrderHistoryRepository orderRepo;
    //private final PaymentRepository paymentRepo;


    @Autowired
    private EmailService emailService;

    // ==============================
    // HIỂN THỊ FORM THANH TOÁN COD
    // ==============================
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
            // lưu t vào biến bên ngoài bằng cách dùng holder — hoặc return t trực tiếp; đơn giản:
            model.addAttribute("cartItems", items);
            model.addAttribute("totalBigDecimal", t);
            model.addAttribute("total", t.intValue()); // dùng int cho URL param
        });

        model.addAttribute("users", user);
        model.addAttribute("address", new Address());
        return "user/orderaddress";
    }


    // ==============================
    // XỬ LÝ SUBMIT COD
    // ==============================
    @PostMapping("/cod")
    public String submitCOD(
            @RequestParam String province_name,
            @RequestParam String district_name,
            @RequestParam String ward_name,
            @RequestParam String addressLine,
            @RequestParam String postalCode,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {

        // 🔹 1. Lấy user đang đăng nhập
        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔹 2. Tạo địa chỉ giao hàng
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


        // ---- Thanh toán đơn hàng bình thường ----
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Lấy danh sách sản phẩm trong giỏ
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

        //Ghép chuỗi sản phẩm (tên + số lượng)
        String orderItems = cartItems.stream()
                .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                .collect(Collectors.joining(", "));

        //Tính tổng tiền thật (tránh lệ thuộc VNPay amount)
        BigDecimal totalAmount = cartItems.stream()
                .map(ci -> BigDecimal.valueOf(ci.getProduct().getItem_price())
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🔹 3. Tạo đơn hàng (liên kết với địa chỉ)
        OrderHistory order = new OrderHistory();
        order.setUser(user);
        order.setAddress(address);// <-- liên kết Address
        order.setOrder_items(orderItems);
        order.setTotal_amount(totalAmount);
        order.setStatus(0);        // pending
        order.setShipping_status(0);
        order.setCreated_at(LocalDateTime.now());
        order.setUpdated_at(LocalDateTime.now());
        orderHistoryRepository.save(order);

        // 🔹 4. Tạo thanh toán COD (liên kết với OrderHistory)
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

        // Xóa giỏ hàng
        cartItemRepository.deleteAll(cartItems);

        redirectAttributes.addFlashAttribute("success", "Ordered successfully");

        return "redirect:/user/home";
    }


    // Gọi sang VNPay tạo đơn hàng
    @GetMapping("/pay")
    public String pay(HttpServletRequest request,
                      @RequestParam("total") int total) throws UnsupportedEncodingException {
        String ipAddress = request.getRemoteAddr();
        String paymentUrl = vnPayService.createOrder(total, "Thanh toán đơn hàng", ipAddress);
        return "redirect:" + paymentUrl;
    }

    // ✅ Thanh toán qua VNPay xong thì TRỪ KHO hàng
    @GetMapping("/payment-return")
    @Transactional
    public String paymentResult(HttpSession session,
                                @RequestParam String province_name,
                                @RequestParam String district_name,
                                @RequestParam String ward_name,
                                @RequestParam String addressLine,
                                @RequestParam String postalCode,
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

            List<Installment> installments = installmentRepository.findUnpaidByInstallmentNo(installmentNo);
            if (installments.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No unpaid installments found for this batch!");
                return "redirect:/user/spay-later";
            }

            installments.forEach(i -> {
                i.setPaid(true);
                i.setPaid_at(LocalDateTime.now());
            });
            installmentRepository.saveAll(installments);

            // Cộng lại hạn mức cho user
            BigDecimal total1 = installments.stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            User insUser = installments.get(0).getPayment().getOrderhistory().getUser();
            insUser.setCredit_limit(insUser.getCredit_limit().add(total1));
            userRepository.save(insUser);
            session.setAttribute("user", insUser);

            redirectAttributes.addFlashAttribute("success", "Installment batch paid successfully!");
            return "redirect:/user/spay-later";
        }
        else if (orderInfo != null && orderInfo.startsWith("all_unpaid_installments")) {
            Long userId = Long.parseLong(orderInfo.split("=")[1]);
            User insUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Installment> unpaid = installmentRepository.findByUserId(userId).stream()
                    .filter(i -> !i.isPaid())
                    .toList();

            unpaid.forEach(i -> {
                i.setPaid(true);
                i.setPaid_at(LocalDateTime.now());
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

        // Tạo order
        String orderItems = cartItems.stream()
                .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                .collect(Collectors.joining(", "));

        // 🔹 2. Tạo địa chỉ giao hàng
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

// ✅ Tạo order có địa chỉ
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


        try {
            emailService.sendOrderSuccessEmail(user.getEmail(), order);
        } catch (Exception ignored) {}

        cartItemRepository.deleteAll(cartItems);

        redirectAttributes.addFlashAttribute("success", "Payment successful via VNPay!");
        return "redirect:/user/home";
    }

    @GetMapping("/spay-later")
    public String viewSpayLater(Model model,
                                @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {

        // 1️⃣ Lấy thông tin user hiện tại
        String username = principal.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 2️⃣ Lấy danh sách kỳ trả góp của user
        List<Installment> installments = installmentRepository.findByUserId(user.getId());

        // 3️⃣ Phân loại hóa đơn: chưa trả và đã trả
        List<Installment> unpaidBills = installments.stream()
                .filter(i -> !i.isPaid())
                .sorted(Comparator.comparing(Installment::getDue_date))
                .collect(Collectors.toList());

        List<Installment> paidBills = installments.stream()
                .filter(Installment::isPaid)
                .sorted(Comparator.comparing(Installment::getPaid_at).reversed())
                .collect(Collectors.toList());

        // 4️⃣ Gom nhóm các kỳ CHƯA TRẢ theo installment_no
        LinkedHashMap<Long, List<Installment>> groupedUnpaidBills = unpaidBills.stream()
                .collect(Collectors.groupingBy(
                        Installment::getInstallment_no,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 5️⃣ Tính toán tổng tiền từng nhóm (UNPAID)
        Map<Long, BigDecimal> principalByGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> feeByGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> lateFeeByGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> grandTotalByGroup = new LinkedHashMap<>();

        for (Map.Entry<Long, List<Installment>> entry : groupedUnpaidBills.entrySet()) {
            BigDecimal principall = entry.getValue().stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal fee = principall.multiply(BigDecimal.valueOf(0.02)); // phí 2%
            BigDecimal lateFee = entry.getValue().stream()
                    .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal total = principall.add(fee).add(lateFee);

            principalByGroup.put(entry.getKey(), principall);
            feeByGroup.put(entry.getKey(), fee);
            lateFeeByGroup.put(entry.getKey(), lateFee);
            grandTotalByGroup.put(entry.getKey(), total);
        }

        // 6️⃣ Gom nhóm các kỳ ĐÃ TRẢ
        LinkedHashMap<Long, List<Installment>> groupedPaidBills = paidBills.stream()
                .collect(Collectors.groupingBy(
                        Installment::getInstallment_no,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 7️⃣ Tính tổng tiền từng nhóm (PAID) — giống logic UNPAID
        Map<Long, BigDecimal> principalByPaidGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> feeByPaidGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> lateFeeByPaidGroup = new LinkedHashMap<>();
        Map<Long, BigDecimal> grandTotalByPaidGroup = new LinkedHashMap<>();

        for (Map.Entry<Long, List<Installment>> entry : groupedPaidBills.entrySet()) {
            BigDecimal principall = entry.getValue().stream()
                    .map(Installment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal fee = principall.multiply(BigDecimal.valueOf(0.02)); // phí 2%
            BigDecimal lateFee = entry.getValue().stream()
                    .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal total = principall.add(fee).add(lateFee);

            principalByPaidGroup.put(entry.getKey(), principall);
            feeByPaidGroup.put(entry.getKey(), fee);
            lateFeeByPaidGroup.put(entry.getKey(), lateFee);
            grandTotalByPaidGroup.put(entry.getKey(), total);
        }

        // 8️⃣ Tìm kỳ chưa trả gần nhất (theo due_date nhỏ nhất)
        List<Installment> nextUnpaidGroup = Collections.emptyList();
        if (!unpaidBills.isEmpty()) {
            Installment nextUnpaid = unpaidBills.get(0);
            Long nextInstallmentNo = nextUnpaid.getInstallment_no();
            nextUnpaidGroup = groupedUnpaidBills.getOrDefault(nextInstallmentNo, Collections.emptyList());
        }

        // 9️⃣ Tính tổng toàn bộ chưa trả
        BigDecimal totalPrincipal = unpaidBills.stream()
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLateFee = unpaidBills.stream()
                .map(i -> Optional.ofNullable(i.getLate_fee()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFee = totalPrincipal.multiply(BigDecimal.valueOf(0.02));
        BigDecimal totalToPay = totalPrincipal.add(totalFee).add(totalLateFee);

        // 🔟 Truyền dữ liệu sang view
        model.addAttribute("user", user);
        model.addAttribute("availableBalance", user.getCredit_limit());

        // danh sách hóa đơn
        model.addAttribute("unpaidBills", unpaidBills);
        model.addAttribute("paidBills", paidBills);

        // nhóm chưa trả
        model.addAttribute("groupedUnpaidBills", groupedUnpaidBills);
        model.addAttribute("principalByGroup", principalByGroup);
        model.addAttribute("feeByGroup", feeByGroup);
        model.addAttribute("lateFeeByGroup", lateFeeByGroup);
        model.addAttribute("grandTotalByGroup", grandTotalByGroup);
        model.addAttribute("nextUnpaidGroup", nextUnpaidGroup);
        model.addAttribute("hasNextUnpaid", !nextUnpaidGroup.isEmpty());

        // nhóm đã trả (giống logic UNPAID)
        model.addAttribute("groupedPaidBills", groupedPaidBills);
        model.addAttribute("principalByPaidGroup", principalByPaidGroup);
        model.addAttribute("feeByPaidGroup", feeByPaidGroup);
        model.addAttribute("lateFeeByPaidGroup", lateFeeByPaidGroup);
        model.addAttribute("grandTotalByPaidGroup", grandTotalByPaidGroup);

        // tổng tiền kỳ hiện tại
        model.addAttribute("currentBills",
                new CurrentBillSummary(totalToPay, totalFee, totalLateFee, unpaidBills));

        return "user/SpayLater";
    }



    // ✅ Trả sau: cũng TRỪ HÀNG
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

        // Ghép items
        String orderItems = cartItems.stream()
                .map(ci -> ci.getProduct().getItem_name() + " x" + ci.getQuantity())
                .collect(Collectors.joining(", "));

        // Tổng tiền
        BigDecimal orderAmount = cartItems.stream()
                .map(ci -> BigDecimal.valueOf(ci.getProduct().getItem_price())
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Trừ hạn mức
        if (user.getCredit_limit().compareTo(orderAmount) < 0) {
            redirectAttributes.addFlashAttribute("error", "Insufficient credit limit");
            return "redirect:/user/cart";
        }

        user.setCredit_limit(user.getCredit_limit().subtract(orderAmount));
        userRepository.save(user);

        // 🔹 2. Tạo địa chỉ giao hàng
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

        // ✅ Tạo order có địa chỉ
        OrderHistory order = new OrderHistory();
        order.setUser(user);
        order.setAddress(address); // ✅ FIX: thêm địa chỉ giao hàng
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

        // Xóa giỏ
        cartItemRepository.deleteAll(cartItems);

        redirectAttributes.addFlashAttribute("success", "Checkout with SpayLater successful!");
        return "redirect:/user/spay-later";
    }


    // Trả từng kỳ qua VNPay
    @GetMapping("/bills/pay/{no}")
    public String payFullInstallment(@PathVariable("no") Long installmentNo,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) throws UnsupportedEncodingException {

        // Lấy tất cả installments trong cùng đợt
        List<Installment> installments = installmentRepository.findUnpaidByInstallmentNo(installmentNo);
        if (installments.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No unpaid installments found for this batch.");
            return "redirect:/user/spay-later";
        }

        // Tính tổng các khoản
        BigDecimal principal = installments.stream()
                .map(Installment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fee = principal.multiply(BigDecimal.valueOf(0.02)); // 2% phí
        BigDecimal lateFee = installments.stream()
                .map(i -> i.getLate_fee() == null ? BigDecimal.ZERO : i.getLate_fee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = principal.add(fee).add(lateFee);

        // Gửi sang VNPay
        String ip = request.getRemoteAddr();
        String orderInfo = "installment:" + installmentNo;
        String url = vnPayService.createOrder(totalAmount.intValue(), orderInfo, ip);

        return "redirect:" + url;
    }


}
