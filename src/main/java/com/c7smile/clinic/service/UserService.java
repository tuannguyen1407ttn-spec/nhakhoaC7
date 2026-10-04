package com.c7smile.clinic.service;

import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Đăng ký tài khoản khách hàng mới với trạng thái chưa kích hoạt (enabled = false).
     * Tạo token xác thực có hiệu lực 24 giờ.
     */
    public User registerCustomer(String email, String fullName, String phone,
                                  LocalDate dob, String rawPassword) {
        User user = new User();
        user.setUsername(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setDob(dob);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole("CUSTOMER");
        user.setEnabled(false); // Cần xác thực email trước khi đăng nhập
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setTokenExpiry(LocalDateTime.now().plusHours(24));
        return userRepository.save(user);
    }

    /**
     * Xác thực email theo token.
     * @return 1 nếu thành công, 0 nếu token không tồn tại, -1 nếu token đã hết hạn
     */
    public int verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            return 0;
        }
        User user = userRepository.findByVerificationToken(token);
        if (user == null) {
            return 0; // Không tìm thấy token
        }
        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            return -1; // Token hết hạn
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
        return 1; // Kích hoạt thành công
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username) != null;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
