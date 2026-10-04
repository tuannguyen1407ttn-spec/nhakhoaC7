package com.c7smile.clinic.service;

import com.c7smile.clinic.entity.*;
import com.c7smile.clinic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ReportService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private DentistRepository dentistRepository;

    public Map<String, Object> generateReport(String range, String customStart, String customEnd) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        if ("today".equalsIgnoreCase(range)) {
            startDate = today;
        } else if ("week".equalsIgnoreCase(range)) {
            startDate = today.minusDays(6);
        } else if ("month".equalsIgnoreCase(range)) {
            startDate = today.withDayOfMonth(1);
        } else if ("year".equalsIgnoreCase(range)) {
            startDate = today.withDayOfYear(1);
        } else if ("custom".equalsIgnoreCase(range) && customStart != null && !customStart.isEmpty()) {
            startDate = LocalDate.parse(customStart);
            if (customEnd != null && !customEnd.isEmpty()) {
                endDate = LocalDate.parse(customEnd);
            }
        } else {
            // "all"
            startDate = LocalDate.of(2020, 1, 1);
        }

        final LocalDate finalStartDate = startDate;
        final LocalDate finalEndDate = endDate;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 1. Lọc hóa đơn trong kỳ
        List<Invoice> allInvoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        List<Invoice> periodInvoices = allInvoices.stream()
                .filter(inv -> inv.getCreatedAt() != null &&
                        !inv.getCreatedAt().isBefore(startDateTime) &&
                        !inv.getCreatedAt().isAfter(endDateTime))
                .collect(Collectors.toList());

        // 2. Lọc các thanh toán thực tế (Payments) trong kỳ
        List<Payment> allPayments = paymentRepository.findAll();
        List<Payment> periodPayments = allPayments.stream()
                .filter(p -> p.getCreatedAt() != null &&
                        !p.getCreatedAt().isBefore(startDateTime) &&
                        !p.getCreatedAt().isAfter(endDateTime))
                .collect(Collectors.toList());

        // Tổng doanh thu thực thu trong kỳ (từ payments hoặc paidAmount)
        double totalRevenue = periodPayments.stream()
                .mapToDouble(Payment::getAmount)
                .sum();

        // Nếu chưa có bảng payments riêng cho hóa đơn cũ thì lấy từ paidAmount của hóa đơn
        if (totalRevenue == 0 && !periodInvoices.isEmpty()) {
            totalRevenue = periodInvoices.stream().mapToDouble(Invoice::getPaidAmount).sum();
        }

        double totalInvoiceAmount = periodInvoices.stream().mapToDouble(Invoice::getTotalAmount).sum();
        double totalDebt = Math.max(0, totalInvoiceAmount - totalRevenue);

        // Doanh thu theo phương thức
        double cashRevenue = periodPayments.stream()
                .filter(p -> p.getMethod() == null || "CASH".equalsIgnoreCase(p.getMethod()))
                .mapToDouble(Payment::getAmount).sum();

        double transferRevenue = periodPayments.stream()
                .filter(p -> p.getMethod() != null && !"CASH".equalsIgnoreCase(p.getMethod()))
                .mapToDouble(Payment::getAmount).sum();

        // 3. Lịch hẹn trong kỳ
        List<Appointment> allAppointments = appointmentRepository.findAll();
        List<Appointment> periodAppointments = allAppointments.stream()
                .filter(a -> a.getAppointmentDate() != null &&
                        !a.getAppointmentDate().isBefore(finalStartDate) &&
                        !a.getAppointmentDate().isAfter(finalEndDate))
                .collect(Collectors.toList());

        long totalAppointments = periodAppointments.size();
        long completedAppointments = periodAppointments.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .count();
        long confirmedAppointments = periodAppointments.stream()
                .filter(a -> "CONFIRMED".equalsIgnoreCase(a.getStatus()))
                .count();
        long pendingAppointments = periodAppointments.stream()
                .filter(a -> "PENDING".equalsIgnoreCase(a.getStatus()))
                .count();
        long cancelledAppointments = periodAppointments.stream()
                .filter(a -> "CANCELLED".equalsIgnoreCase(a.getStatus()))
                .count();

        // Nguồn khách hàng
        long onlineCount = periodAppointments.stream()
                .filter(a -> a.getSource() == null || "ONLINE".equalsIgnoreCase(a.getSource()))
                .count();
        long walkInCount = periodAppointments.stream()
                .filter(a -> "WALK_IN".equalsIgnoreCase(a.getSource()))
                .count();

        // 4. Số bệnh nhân được phục vụ trong kỳ
        long patientsServed = periodAppointments.stream()
                .map(Appointment::getPatient)
                .filter(Objects::nonNull)
                .map(Patient::getId)
                .distinct()
                .count();
        if (patientsServed == 0) {
            patientsServed = patientRepository.count();
        }

        // 5. Chuẩn bị dữ liệu biểu đồ doanh thu theo 7 ngày gần nhất
        List<String> chartLabels = new ArrayList<>();
        List<Double> chartRevenue = new ArrayList<>();
        List<Long> chartAppts = new ArrayList<>();

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String label = d.format(dayFormatter);
            chartLabels.add(label);

            double dayRev = allPayments.stream()
                    .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().isEqual(d))
                    .mapToDouble(Payment::getAmount)
                    .sum();
            chartRevenue.add(dayRev);

            long dayAppt = allAppointments.stream()
                    .filter(a -> a.getAppointmentDate() != null && a.getAppointmentDate().isEqual(d))
                    .count();
            chartAppts.add(dayAppt);
        }

        // 6. Thống kê dịch vụ phổ biến (Top Services)
        Map<String, ServiceStat> serviceStatsMap = new HashMap<>();
        for (Appointment appt : periodAppointments) {
            if (appt.getService() != null) {
                String sName = appt.getService().getName();
                double sPrice = appt.getService().getPrice() != null ? appt.getService().getPrice() : 0.0;
                ServiceStat stat = serviceStatsMap.computeIfAbsent(sName, k -> new ServiceStat(sName, sPrice));
                stat.count++;
                stat.totalAmount += sPrice;
            }
        }
        List<ServiceStat> topServices = new ArrayList<>(serviceStatsMap.values());
        topServices.sort((a, b) -> Double.compare(b.totalAmount, a.totalAmount));

        // 7. Thống kê bác sĩ (Doctor Performance) từ hồ sơ bệnh án hoặc danh sách bác sĩ
        List<MedicalRecord> allRecords = medicalRecordRepository.findAll();
        Map<String, DoctorStat> doctorStatsMap = new HashMap<>();

        for (MedicalRecord mr : allRecords) {
            if (mr.getDoctor() != null) {
                String docName = mr.getDoctor().getFullName() != null ? mr.getDoctor().getFullName() : mr.getDoctor().getUsername();
                DoctorStat dStat = doctorStatsMap.computeIfAbsent(docName, k -> new DoctorStat(docName, "Bác sĩ điều trị"));
                dStat.appointmentCount++;
                dStat.completedCount++;
                if (mr.getAppointment() != null && mr.getAppointment().getService() != null && mr.getAppointment().getService().getPrice() != null) {
                    dStat.estimatedRevenue += mr.getAppointment().getService().getPrice();
                }
            }
        }

        // Bổ sung các bác sĩ trong danh mục nếu chưa có ca khám
        List<Dentist> dentists = dentistRepository.findAll();
        for (Dentist d : dentists) {
            String role = d.getPosition() != null ? d.getPosition() : (d.getExpertise() != null ? d.getExpertise() : "Nha sĩ chuyên khoa");
            doctorStatsMap.putIfAbsent(d.getFullName(), new DoctorStat(d.getFullName(), role));
        }

        List<DoctorStat> doctorStats = new ArrayList<>(doctorStatsMap.values());
        doctorStats.sort((a, b) -> Integer.compare(b.appointmentCount, a.appointmentCount));

        // Đóng gói kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("range", range != null ? range : "month");
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalRevenue", totalRevenue);
        result.put("totalInvoiceAmount", totalInvoiceAmount);
        result.put("totalDebt", totalDebt);
        result.put("cashRevenue", cashRevenue);
        result.put("transferRevenue", transferRevenue);
        result.put("invoiceCount", periodInvoices.size());
        result.put("paidInvoiceCount", periodInvoices.stream().filter(i -> "PAID".equalsIgnoreCase(i.getStatus())).count());
        
        result.put("totalAppointments", totalAppointments);
        result.put("completedAppointments", completedAppointments);
        result.put("confirmedAppointments", confirmedAppointments);
        result.put("pendingAppointments", pendingAppointments);
        result.put("cancelledAppointments", cancelledAppointments);
        result.put("onlineCount", onlineCount);
        result.put("walkInCount", walkInCount);
        result.put("newPatients", patientsServed);

        result.put("chartLabels", chartLabels);
        result.put("chartRevenue", chartRevenue);
        result.put("chartAppts", chartAppts);

        result.put("topServices", topServices);
        result.put("doctorStats", doctorStats);
        result.put("recentInvoices", periodInvoices.stream().limit(10).collect(Collectors.toList()));

        return result;
    }

    public static class ServiceStat {
        public String name;
        public double unitPrice;
        public int count = 0;
        public double totalAmount = 0.0;

        public ServiceStat(String name, double unitPrice) {
            this.name = name;
            this.unitPrice = unitPrice;
        }

        public String getName() { return name; }
        public double getUnitPrice() { return unitPrice; }
        public int getCount() { return count; }
        public double getTotalAmount() { return totalAmount; }
    }

    public static class DoctorStat {
        public String name;
        public String specialty;
        public int appointmentCount = 0;
        public int completedCount = 0;
        public double estimatedRevenue = 0.0;

        public DoctorStat(String name, String specialty) {
            this.name = name;
            this.specialty = specialty;
        }

        public String getName() { return name; }
        public String getSpecialty() { return specialty; }
        public int getAppointmentCount() { return appointmentCount; }
        public int getCompletedCount() { return completedCount; }
        public double getEstimatedRevenue() { return estimatedRevenue; }
    }
}
