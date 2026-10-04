package com.c7smile.clinic.controller;

import com.c7smile.clinic.entity.Appointment;
import com.c7smile.clinic.entity.Dentist;
import com.c7smile.clinic.entity.Service;
import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.repository.AppointmentRepository;
import com.c7smile.clinic.service.ClinicService;
import com.c7smile.clinic.service.DentistService;
import com.c7smile.clinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin Controller — URL /admin/** được bảo vệ bởi Spring Security (chỉ
 * ROLE_ADMIN).
 * Không cần kiểm tra session thủ công.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private DentistService dentistService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.c7smile.clinic.service.ReportService reportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("serviceCount", clinicService.count());
        model.addAttribute("dentistCount", dentistService.count());
        model.addAttribute("appointmentCount", appointmentRepository.count());
        model.addAttribute("userCount", userRepository.count());

        // Doanh thu tóm tắt tháng này
        Map<String, Object> monthReport = reportService.generateReport("month", null, null);
        model.addAttribute("totalRevenue", monthReport.get("totalRevenue"));
        model.addAttribute("totalDebt", monthReport.get("totalDebt"));
        return "admin/dashboard";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        Map<String, Object> report = reportService.generateReport(range, startDate, endDate);
        model.addAllAttributes(report);
        return "admin/reports";
    }

    // --- SERVICE CRUD ---
    @GetMapping("/services")
    public String listServices(Model model) {
        model.addAttribute("services", clinicService.getAllServices());
        return "admin/services";
    }

    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            return null;
        try {
            String originalFilename = file.getOriginalFilename();
            String sanitized = originalFilename == null ? "file"
                    : originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String filename = UUID.randomUUID().toString() + "_" + sanitized;
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path path = uploadDir.resolve(filename);
            Files.write(path, file.getBytes());
            return "/uploads/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/services/save")
    public String saveService(@ModelAttribute Service service,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        if (service.getId() != null) {
            Service existing = clinicService.getServiceById(service.getId()).orElse(null);
            if (existing != null) {
                if (imageFile == null || imageFile.isEmpty()) {
                    service.setImageUrl(existing.getImageUrl());
                }
                if (service.getSlug() == null)
                    service.setSlug(existing.getSlug());
            }
        }

        String uploadedFile = saveFile(imageFile);
        if (uploadedFile != null) {
            service.setImageUrl(uploadedFile);
        }

        clinicService.saveService(service);
        return "redirect:/admin/services";
    }

    @GetMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Unlink all appointments referencing this service before deleting
            List<Appointment> linkedAppointments = appointmentRepository.findByServiceId(id);
            for (Appointment appt : linkedAppointments) {
                appt.setService(null);
                appointmentRepository.save(appt);
            }
            clinicService.deleteService(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    // --- DENTIST CRUD ---
    @GetMapping("/dentists")
    public String listDentists(Model model) {
        model.addAttribute("dentists", dentistService.getAllDentists());
        return "admin/dentists";
    }

    @PostMapping("/dentists/save")
    public String saveDentist(@ModelAttribute Dentist dentist,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        if (dentist.getId() != null) {
            Dentist existing = dentistService.getDentistById(dentist.getId()).orElse(null);
            if (existing != null) {
                if (imageFile == null || imageFile.isEmpty()) {
                    dentist.setAvatarUrl(existing.getAvatarUrl());
                }
                if (dentist.getSlug() == null)
                    dentist.setSlug(existing.getSlug());
            }
        }

        String uploadedFile = saveFile(imageFile);
        if (uploadedFile != null) {
            dentist.setAvatarUrl(uploadedFile);
        }

        dentist = dentistService.saveDentist(dentist);

        // Tự động sinh hoặc cập nhật tài khoản cho bác sĩ: username = email, password = 123456, role = DOCTOR
        if (dentist.getEmail() != null && !dentist.getEmail().trim().isEmpty()) {
            String email = dentist.getEmail().trim().toLowerCase();
            User user = userRepository.findByUsername(email);
            if (user == null) {
                user = new User();
                user.setUsername(email);
            }
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRole("DOCTOR");
            user.setEnabled(true);
            user.setFullName(dentist.getFullName());
            if (dentist.getPhone() != null && !dentist.getPhone().trim().isEmpty()) {
                user.setPhone(dentist.getPhone().trim());
            }
            if (dentist.getDob() != null) {
                user.setDob(dentist.getDob());
            }
            userRepository.save(user);
        }

        return "redirect:/admin/dentists";
    }

    @GetMapping("/dentists/delete/{id}")
    public String deleteDentist(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            dentistService.deleteDentist(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bác sĩ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa bác sĩ: " + e.getMessage());
        }
        return "redirect:/admin/dentists";
    }

    // --- PATIENT CRUD ---
    @Autowired
    private com.c7smile.clinic.service.PatientService patientService;

    @GetMapping("/patients")
    public String listPatients(Model model) {
        model.addAttribute("patients", patientService.getAllPatients());
        return "admin/patients";
    }

    @PostMapping("/patients/save")
    public String savePatient(@ModelAttribute com.c7smile.clinic.entity.Patient patient) {
        patientService.savePatient(patient);
        return "redirect:/admin/patients";
    }

    @GetMapping("/patients/delete/{id}")
    public String deletePatient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            patientService.deletePatient(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa hồ sơ bệnh nhân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa hồ sơ: " + e.getMessage());
        }
        return "redirect:/admin/patients";
    }

    // --- APPOINTMENT MANAGEMENT ---
    @Autowired
    private com.c7smile.clinic.service.AppointmentService appointmentService;

    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointmentsSorted());
        return "admin/appointments";
    }

    @PostMapping("/appointments/status")
    public String updateAppointmentStatus(@RequestParam Long id, @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            appointmentService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/appointments";
    }
}
