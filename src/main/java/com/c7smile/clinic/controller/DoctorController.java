package com.c7smile.clinic.controller;

import com.c7smile.clinic.entity.Appointment;
import com.c7smile.clinic.entity.MedicalRecord;
import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.repository.AppointmentRepository;
import com.c7smile.clinic.repository.UserRepository;
import com.c7smile.clinic.service.AppointmentService;
import com.c7smile.clinic.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Lấy lịch hẹn trong ngày
        List<Appointment> todayAppointments = appointmentService.getAppointmentsForToday();
        
        // Bác sĩ chỉ quan tâm lịch PENDING, CONFIRMED (chuẩn bị khám), COMPLETED (vừa khám xong)
        model.addAttribute("appointments", todayAppointments);
        return "doctor/dashboard";
    }

    @GetMapping("/examine/{appointmentId}")
    public String examineForm(@PathVariable Long appointmentId, Model model, RedirectAttributes redirectAttributes) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy lịch hẹn");
            return "redirect:/doctor/dashboard";
        }
        
        // Kiểm tra xem đã khám chưa (tránh ghi đè nếu chưa cần thiết, hoặc cho phép sửa)
        MedicalRecord record = medicalRecordService.getRecordByAppointmentId(appointmentId).orElse(new MedicalRecord());
        
        // Lấy lịch sử khám bệnh của bệnh nhân
        List<MedicalRecord> history = medicalRecordService.getRecordsByPatientId(appointment.getPatient().getId());
        
        model.addAttribute("appointment", appointment);
        model.addAttribute("record", record);
        model.addAttribute("history", history);
        
        return "doctor/examine";
    }

    @PostMapping("/examine/save")
    public String saveExamination(
            @RequestParam Long appointmentId,
            @RequestParam String symptoms,
            @RequestParam String diagnosis,
            @RequestParam String treatment,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
            
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
            User doctor = userRepository.findByUsername(authentication.getName());
            
            MedicalRecord record = medicalRecordService.getRecordByAppointmentId(appointmentId).orElse(new MedicalRecord());
            record.setAppointment(appointment);
            record.setPatient(appointment.getPatient());
            record.setDoctor(doctor);
            record.setSymptoms(symptoms);
            record.setDiagnosis(diagnosis);
            record.setTreatment(treatment);
            record.setNotes(notes);
            
            medicalRecordService.saveRecord(record);
            
            // Đánh dấu lịch hẹn đã hoàn thành
            appointmentService.updateStatus(appointmentId, "COMPLETED");
            
            redirectAttributes.addFlashAttribute("successMessage", "Lưu hồ sơ khám bệnh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/doctor/dashboard";
    }
    
    @GetMapping("/medical-records")
    public String myMedicalRecords(Authentication authentication, Model model) {
        User doctor = userRepository.findByUsername(authentication.getName());
        List<MedicalRecord> records = medicalRecordService.getRecordsByDoctorId(doctor.getId());
        model.addAttribute("records", records);
        return "doctor/medical-records";
    }
}
