package com.c7smile.clinic.repository;

import com.c7smile.clinic.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Invoice> findByPatientIdInOrderByCreatedAtDesc(java.util.Collection<Long> patientIds);
    List<Invoice> findAllByOrderByCreatedAtDesc();
    Optional<Invoice> findByAppointmentId(Long appointmentId);
    List<Invoice> findByStatusNot(String status);
}
