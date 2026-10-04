package com.c7smile.clinic.service;

import com.c7smile.clinic.entity.Invoice;
import com.c7smile.clinic.entity.Payment;
import com.c7smile.clinic.repository.InvoiceRepository;
import com.c7smile.clinic.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Invoice> getPendingInvoices() {
        return invoiceRepository.findByStatusNot("PAID");
    }
    
    public List<Invoice> getInvoicesByPatient(Long patientId) {
        return invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public Invoice createPaidInvoice(com.c7smile.clinic.entity.Appointment appointment, Double totalAmount, String method) {
        Optional<Invoice> existing = invoiceRepository.findByAppointmentId(appointment.getId());
        Invoice invoice;
        if (existing.isPresent()) {
            invoice = existing.get();
            invoice.setTotalAmount(totalAmount);
            invoice.setPaidAmount(totalAmount);
            invoice.setStatus("PAID");
            invoice = invoiceRepository.save(invoice);
        } else {
            invoice = new Invoice();
            invoice.setAppointment(appointment);
            invoice.setPatient(appointment.getPatient());
            invoice.setTotalAmount(totalAmount);
            invoice.setPaidAmount(totalAmount);
            invoice.setStatus("PAID");
            invoice = invoiceRepository.save(invoice);
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(totalAmount);
        payment.setMethod(method != null ? method : "CASH");
        paymentRepository.save(payment);

        return invoice;
    }

    public Payment addPayment(Long invoiceId, Double amount, String method) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment = paymentRepository.save(payment);
        
        // Cập nhật hóa đơn
        Double newPaid = invoice.getPaidAmount() + amount;
        invoice.setPaidAmount(newPaid);
        
        if (newPaid >= invoice.getTotalAmount()) {
            invoice.setStatus("PAID");
        } else if (newPaid > 0) {
            invoice.setStatus("PARTIALLY_PAID");
        }
        
        invoiceRepository.save(invoice);
        
        return payment;
    }
    
    public List<Payment> getPaymentsForInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
    }
}
