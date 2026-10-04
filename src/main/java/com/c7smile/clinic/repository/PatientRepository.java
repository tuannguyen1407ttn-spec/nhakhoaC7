package com.c7smile.clinic.repository;

import com.c7smile.clinic.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByAccountId(Long accountId);
    
    // Dùng để tìm kiếm bệnh nhân cho lễ tân / bác sĩ
    List<Patient> findByFullNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);
    
    // Dùng cho luồng Existing Patient Found khi đăng ký online
    List<Patient> findByFullNameAndPhoneAndDob(String fullName, String phone, java.time.LocalDate dob);
}
