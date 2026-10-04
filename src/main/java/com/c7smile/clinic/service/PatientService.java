package com.c7smile.clinic.service;

import com.c7smile.clinic.entity.Patient;
import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    public List<Patient> getPatientsByAccountId(Long accountId) {
        return patientRepository.findByAccountId(accountId);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        patientRepository.deleteById(id);
    }

    public List<Patient> searchPatients(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPatients();
        }
        return patientRepository.findByFullNameContainingIgnoreCaseOrPhoneContaining(keyword, keyword);
    }

    /**
     * Tự động tạo hồ sơ bệnh nhân khi một tài khoản (User) mới được đăng ký
     * và chưa có Patient nào khớp (để tự động link).
     */
    public Patient createPatientFromUser(User user) {
        Patient p = new Patient();
        p.setAccount(user);
        p.setFullName(user.getFullName());
        p.setPhone(user.getPhone());
        p.setDob(user.getDob());
        p.setEmail(user.getUsername());
        return patientRepository.save(p);
    }
}
