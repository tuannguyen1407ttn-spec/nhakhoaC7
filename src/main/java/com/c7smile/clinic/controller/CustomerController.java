package com.c7smile.clinic.controller;

import com.c7smile.clinic.entity.Appointment;
import com.c7smile.clinic.entity.Invoice;
import com.c7smile.clinic.entity.MedicalRecord;
import com.c7smile.clinic.entity.Patient;
import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.repository.AppointmentRepository;
import com.c7smile.clinic.repository.InvoiceRepository;
import com.c7smile.clinic.repository.MedicalRecordRepository;
import com.c7smile.clinic.repository.UserRepository;
import com.c7smile.clinic.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    /**
     * Lấy danh sách patient IDs của user hiện tại.
     */
    private List<Long> getPatientIds(User user) {
        List<Patient> patients = patientService.getPatientsByAccountId(user.getId());
        if (patients == null || patients.isEmpty()) return Collections.emptyList();
        return patients.stream().map(Patient::getId).collect(Collectors.toList());
    }

    /**
     * Giao diện duy nhất, liền mạch cho Khách hàng (Unified Customer Portal).
     * Tích hợp: Tổng quan & Lịch hẹn, Hồ sơ bệnh nhân, Lịch sử khám, Hóa đơn & Thanh toán.
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model,
                            @RequestParam(value = "bookingSuccess", required = false) String bookingSuccess,
                            @RequestParam(value = "tab", required = false) String tab) {
        User user = userRepository.findByUsername(authentication.getName());
        List<Appointment> appointments = appointmentRepository.findByUserId(user.getId());
        List<Long> patientIds = getPatientIds(user);
        List<Patient> patients = patientService.getPatientsByAccountId(user.getId());

        List<MedicalRecord> records = Collections.emptyList();
        List<Invoice> invoices = Collections.emptyList();
        if (!patientIds.isEmpty()) {
            records = medicalRecordRepository.findByPatientIdInOrderByCreatedAtDesc(patientIds);
            invoices = invoiceRepository.findByPatientIdInOrderByCreatedAtDesc(patientIds);
        }

        model.addAttribute("loggedInUser", user);
        model.addAttribute("appointments", appointments != null ? appointments : Collections.emptyList());
        model.addAttribute("patients", patients != null ? patients : Collections.emptyList());
        model.addAttribute("records", records);
        model.addAttribute("invoices", invoices);

        model.addAttribute("appointmentCount", appointments != null ? appointments.size() : 0);
        model.addAttribute("patientCount", patients != null ? patients.size() : 0);
        model.addAttribute("medicalRecordCount", records.size());
        model.addAttribute("invoiceCount", invoices.size());

        model.addAttribute("activeTab", (tab != null && !tab.isEmpty()) ? tab : "overview");

        if (bookingSuccess != null) {
            model.addAttribute("successMessage", "Đặt lịch khám thành công! Chúng tôi sẽ sớm liên hệ để xác nhận.");
        }
        return "customer/dashboard";
    }

    @GetMapping("/patients")
    public String listPatients() {
        return "redirect:/customer/dashboard?tab=patients";
    }

    @PostMapping("/patients/save")
    public String savePatient(Authentication authentication,
                              @ModelAttribute Patient patient) {
        User user = userRepository.findByUsername(authentication.getName());
        patient.setAccount(user);
        patientService.savePatient(patient);
        return "redirect:/customer/dashboard?tab=patients";
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/login";
            }

            user.setFullName(fullName);
            user.setPhone(phone);

            // Xử lý đổi mật khẩu nếu có nhập
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                    redirectAttributes.addFlashAttribute("profileError", "Mật khẩu hiện tại không chính xác!");
                    return "redirect:/customer/dashboard?tab=profile";
                }
                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("profileError", "Mật khẩu mới và xác nhận mật khẩu không khớp!");
                    return "redirect:/customer/dashboard?tab=profile";
                }
                if (newPassword.length() < 6) {
                    redirectAttributes.addFlashAttribute("profileError", "Mật khẩu mới phải có ít nhất 6 ký tự!");
                    return "redirect:/customer/dashboard?tab=profile";
                }
                user.setPassword(passwordEncoder.encode(newPassword));
            }

            userRepository.save(user);
            redirectAttributes.addFlashAttribute("profileSuccess", "Cập nhật thông tin cá nhân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("profileError", "Lỗi: " + e.getMessage());
        }
        return "redirect:/customer/dashboard?tab=profile";
    }

    @GetMapping("/profile")
    public String profileRedirect() {
        return "redirect:/customer/dashboard?tab=profile";
    }

    @GetMapping("/medical-records")
    public String medicalRecords() {
        return "redirect:/customer/dashboard?tab=records";
    }

    @GetMapping("/invoices")
    public String invoices() {
        return "redirect:/customer/dashboard?tab=invoices";
    }
}

