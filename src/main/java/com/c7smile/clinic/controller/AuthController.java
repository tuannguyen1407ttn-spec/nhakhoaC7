package com.c7smile.clinic.controller;

import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.service.EmailService;
import com.c7smile.clinic.service.PatientService;
import com.c7smile.clinic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private EmailService emailService;

    /**
     * Spring Security xử lý POST /login.
     * Controller hiển thị trang đăng nhập với các trạng thái kích hoạt / lỗi rõ ràng.
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "verified", required = false) String verified,
                            Model model) {
        if (error != null) {
            if ("disabled".equalsIgnoreCase(error)) {
                model.addAttribute("error", "Tài khoản chưa được kích hoạt! Vui lòng kiểm tra hộp thư email (cả mục Spam) để bấm liên kết xác thực tài khoản.");
            } else if ("invalid_token".equalsIgnoreCase(error)) {
                model.addAttribute("error", "Liên kết xác thực không hợp lệ hoặc đã từng được sử dụng.");
            } else if ("expired_token".equalsIgnoreCase(error)) {
                model.addAttribute("error", "Liên kết xác thực đã hết hạn (quá 24 giờ). Vui lòng đăng ký lại.");
            } else {
                model.addAttribute("error", "Email hoặc mật khẩu không chính xác.");
            }
        }
        if (logout != null) {
            model.addAttribute("message", "Đã đăng xuất thành công.");
        }
        if (registered != null) {
            model.addAttribute("message", "Đăng ký thành công! Một email xác thực đã được gửi tới hộp thư của bạn. Vui lòng kiểm tra email để kích hoạt tài khoản.");
        }
        if (verified != null) {
            model.addAttribute("message", "Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String fullName,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String dob,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           HttpServletRequest request,
                           Model model) {
        // Validate password match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            preserveFormData(model, username, fullName, phone, dob);
            return "register";
        }

        // Validate username uniqueness
        if (userService.existsByUsername(username)) {
            model.addAttribute("error", "Email đã được sử dụng. Vui lòng chọn email khác.");
            preserveFormData(model, null, fullName, phone, dob);
            return "register";
        }

        // Parse date of birth
        LocalDate dateOfBirth = null;
        if (dob != null && !dob.isEmpty()) {
            try {
                dateOfBirth = LocalDate.parse(dob);
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }

        User newUser = userService.registerCustomer(username, fullName, phone, dateOfBirth, password);
        
        // Tạo hồ sơ bệnh nhân mặc định cho user vừa đăng ký
        patientService.createPatientFromUser(newUser);

        // Gửi email xác thực tài khoản
        String baseUrl = getBaseUrl(request);
        emailService.sendVerificationEmail(newUser.getUsername(), newUser.getFullName(), newUser.getVerificationToken(), baseUrl);
        
        return "redirect:/login?registered=pending";
    }

    /**
     * Endpoint xử lý khi người dùng nhấn link kích hoạt từ email.
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token) {
        int result = userService.verifyEmail(token);
        if (result == 1) {
            return "redirect:/login?verified=true";
        } else if (result == -1) {
            return "redirect:/login?error=expired_token";
        } else {
            return "redirect:/login?error=invalid_token";
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);
        return url.toString();
    }

    private void preserveFormData(Model model, String username, String fullName, String phone, String dob) {
        model.addAttribute("username", username);
        model.addAttribute("fullName", fullName);
        model.addAttribute("phone", phone);
        model.addAttribute("dob", dob);
    }
}
