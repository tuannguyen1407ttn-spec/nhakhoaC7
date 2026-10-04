package com.c7smile.clinic.repository;

import com.c7smile.clinic.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserId(Long userId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByServiceId(Long serviceId);

    List<Appointment> findAllByOrderByAppointmentDateDescTimeSlotAsc();

    List<Appointment> findByAppointmentDateOrderByTimeSlotAsc(java.time.LocalDate date);
}
