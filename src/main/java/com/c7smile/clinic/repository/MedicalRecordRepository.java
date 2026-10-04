package com.c7smile.clinic.repository;

import com.c7smile.clinic.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<MedicalRecord> findByPatientIdInOrderByCreatedAtDesc(java.util.Collection<Long> patientIds);
    List<MedicalRecord> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);
}
