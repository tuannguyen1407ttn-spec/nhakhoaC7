package com.c7smile.clinic.controller;

import com.c7smile.clinic.entity.Appointment;
import com.c7smile.clinic.entity.Patient;
import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.service.AppointmentService;
import com.c7smile.clinic.service.ClinicService;
import com.c7smile.clinic.service.EmailService;
import com.c7smile.clinic.service.PatientService;
import com.c7smile.clinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class BookingController {

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/booking")
    public String bookingPage(Authentication authentication, Model model) {
        User loggedInUser = getUser(authentication);
        if (loggedInUser == null) {
            return "redirect:/login"; // Bắt buộc đăng nhập để đặt lịch online
        }

        List<com.c7smile.clinic.entity.Service> services = clinicService.getAllServices();
        List<Patient> patients = patientService.getPatientsByAccountId(loggedInUser.getId());
        
        model.addAttribute("services", services);
        model.addAttribute("patients", patients);
        model.addAttribute("user", loggedInUser);
        return "booking";
    }

    @PostMapping("/booking")
    public String makeBooking(@RequestParam Long patientId,
                              @RequestParam Long serviceId, 
                              @RequestParam String appointmentDate,
                              @RequestParam String timeSlot, 
                              @RequestParam(required = false) String notes,
                              Authentication authentication) {
        User loggedInUser = getUser(authentication);
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        Appointment appointment = new Appointment();
        appointment.setUser(loggedInUser); // Người đặt lịch
        
        // Gắn Patient thực tế
        patientService.getPatientById(patientId).ifPresent(appointment::setPatient);
        
        // Gắn Dịch vụ
        clinicService.getServiceById(serviceId).ifPresent(appointment::setService);

        appointment.setAppointmentDate(LocalDate.parse(appointmentDate));
        appointment.setTimeSlot(timeSlot);
        appointment.setNotes(notes);
        appointment.setSource("ONLINE");
        appointment.setStatus("PENDING");

        appointmentService.saveAppointment(appointment);

        // Gửi email thông báo đặt lịch khám thành công
        String targetEmail = loggedInUser.getUsername();
        if (targetEmail != null && targetEmail.contains("@")) {
            emailService.sendBookingConfirmationEmail(targetEmail, appointment);
        }

        return "redirect:/customer/dashboard?bookingSuccess=true";
    }

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        return "redirect:/customer/dashboard"; // Migrate trang profile sang customer dashboard
    }

    private User getUser(Authentication authentication) {
        if (authentication == null) return null;
        return userRepository.findByUsername(authentication.getName());
    }
}
