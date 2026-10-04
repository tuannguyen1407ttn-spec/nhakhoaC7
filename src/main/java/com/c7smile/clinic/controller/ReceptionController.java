package com.c7smile.clinic.controller;

import com.c7smile.clinic.entity.Appointment;
import com.c7smile.clinic.entity.Invoice;
import com.c7smile.clinic.entity.Patient;
import com.c7smile.clinic.entity.User;
import com.c7smile.clinic.service.AppointmentService;
import com.c7smile.clinic.service.ClinicService;
import com.c7smile.clinic.service.InvoiceService;
import com.c7smile.clinic.service.PatientService;
import com.c7smile.clinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reception")
public class ReceptionController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private com.c7smile.clinic.repository.InvoiceRepository invoiceRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Appointments today for Check-in
        List<Appointment> todayAppointments = appointmentService.getAppointmentsForToday();
        
        // Lấy danh sách ID các lịch hẹn đã có hóa đơn để không bị trùng
        List<Long> invoicedAppointmentIds = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getAppointment() != null)
                .map(inv -> inv.getAppointment().getId())
                .collect(Collectors.toList());

        // Completed appointments waiting for invoice (chưa xuất hóa đơn)
        List<Appointment> completedAppointments = todayAppointments.stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .filter(a -> !invoicedAppointmentIds.contains(a.getId()))
                .collect(Collectors.toList());
                
        // Pending invoices waiting for payment (chưa thanh toán đủ)
        List<Invoice> pendingInvoices = invoiceService.getPendingInvoices();
        
        model.addAttribute("appointmentsToday", todayAppointments);
        model.addAttribute("completedAppointments", completedAppointments);
        model.addAttribute("pendingInvoices", pendingInvoices);
        return "reception/dashboard";
    }

    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointmentsSorted());
        return "reception/appointments";
    }

    @PostMapping("/appointments/status")
    public String updateAppointmentStatus(@RequestParam Long id, @RequestParam String status, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/reception/appointments";
    }

    @GetMapping("/walk-in")
    public String walkInForm(Model model) {
        model.addAttribute("services", clinicService.getAllServices());
        model.addAttribute("patients", patientService.getAllPatients());
        return "reception/walk-in";
    }

    @PostMapping("/walk-in/save")
    public String saveWalkIn(
            @RequestParam(required = false) Long existingPatientId,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String dob,
            @RequestParam Long serviceId,
            @RequestParam String appointmentDate,
            @RequestParam String timeSlot,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            Patient patient;
            if (existingPatientId != null && existingPatientId > 0) {
                patient = patientService.getPatientById(existingPatientId).orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân"));
            } else {
                if (fullName == null || fullName.trim().isEmpty()) {
                    throw new RuntimeException("Vui lòng nhập tên khách hàng mới.");
                }
                patient = new Patient();
                patient.setFullName(fullName);
                patient.setPhone(phone);
                patient.setGender(gender);
                if (dob != null && !dob.isEmpty()) {
                    patient.setDob(LocalDate.parse(dob));
                }
                patient = patientService.savePatient(patient);
            }

            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            clinicService.getServiceById(serviceId).ifPresent(appointment::setService);
            appointment.setAppointmentDate(LocalDate.parse(appointmentDate));
            appointment.setTimeSlot(timeSlot);
            appointment.setNotes(notes);
            appointment.setSource("WALK_IN");
            appointment.setStatus("CONFIRMED");
            
            User loggedInUser = userRepository.findByUsername(authentication.getName());
            appointment.setUser(loggedInUser);

            appointmentService.saveAppointment(appointment);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch tiếp nhận khách vãng lai thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo lịch hẹn: " + e.getMessage());
        }
        return "redirect:/reception/walk-in";
    }

    // ==========================================
    // CASHIER/BILLING FUNCTIONALITY MERGED
    // ==========================================

    @PostMapping("/invoices/create")
    public String createInvoice(@RequestParam Long appointmentId,
                                @RequestParam Double totalAmount,
                                @RequestParam(value = "method", defaultValue = "CASH") String method,
                                RedirectAttributes redirectAttributes) {
        try {
            Appointment appointment = appointmentService.getAllAppointmentsSorted().stream()
                .filter(a -> a.getId().equals(appointmentId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
            
            invoiceService.createPaidInvoice(appointment, totalAmount, method);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xuất hóa đơn thành công! Trạng thái: PAID.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/reception/dashboard";
    }

    @GetMapping("/invoices")
    public String listInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "reception/invoices";
    }

    @GetMapping("/invoices/{id}")
    public String invoiceDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Invoice invoice = invoiceService.getInvoiceById(id).orElse(null);
        if (invoice == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hóa đơn");
            return "redirect:/reception/invoices";
        }
        
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", invoiceService.getPaymentsForInvoice(id));
        return "reception/payment";
    }

    @PostMapping("/payments/save")
    public String savePayment(@RequestParam Long invoiceId, @RequestParam Double amount, @RequestParam String method, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.addPayment(invoiceId, amount, method);
            redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận thanh toán thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/reception/invoices/" + invoiceId;
    }
}
