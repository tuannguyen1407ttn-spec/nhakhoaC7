package com.c7smile.clinic.repository;

import com.c7smile.clinic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByPhone(String phone);
    List<User> findByFullNameAndPhone(String fullName, String phone);
    User findByVerificationToken(String verificationToken);
}
