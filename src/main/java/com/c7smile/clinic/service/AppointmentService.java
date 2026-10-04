package com.c7smile.clinic.service;

import com.c7smile.clinic.entity.Appointment;
import com.c7smile.clinic.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Appointment> getAppointmentsByUserId(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }
    
    public List<Appointment> getAppointmentsByPatientId(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAllAppointmentsSorted() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescTimeSlotAsc();
    }

    public List<Appointment> getAppointmentsForToday() {
        return appointmentRepository.findByAppointmentDateOrderByTimeSlotAsc(java.time.LocalDate.now());
    }

    public Appointment saveAppointment(Appointment appointment) {
        return appointmentRepository.save(appointment);
    }
    
    public void updateStatus(Long id, String status) {
        appointmentRepository.findById(id).ifPresent(appt -> {
            appt.setStatus(status);
            appointmentRepository.save(appt);
        });
    }
    
    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }
}
