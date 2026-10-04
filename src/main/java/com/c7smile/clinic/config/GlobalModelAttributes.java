package com.c7smile.clinic.config;

import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Cung cấp loggedInUser và userRole cho TẤT CẢ Thymeleaf templates.
 * Các template hiện tại dùng ${loggedInUser} sẽ tiếp tục hoạt động
 * mà không cần sửa code.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("loggedInUser")
    public User loggedInUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return userRepository.findByUsername(authentication.getName());
        }
        return null;
    }

    @ModelAttribute("userRole")
    public String userRole(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse(null);
        }
        return null;
    }
}
