package com.c7smile.clinic.service;

import com.c7smile.clinic.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClinicService {

    @Autowired
    private ServiceRepository serviceRepository;

    public List<com.c7smile.clinic.entity.Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public List<com.c7smile.clinic.entity.Service> getTopServices() {
        return serviceRepository.findTop4ByOrderByIdAsc();
    }

    public Optional<com.c7smile.clinic.entity.Service> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    public Optional<com.c7smile.clinic.entity.Service> getServiceBySlug(String slug) {
        return serviceRepository.findBySlug(slug);
    }

    public com.c7smile.clinic.entity.Service saveService(com.c7smile.clinic.entity.Service service) {
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    public long count() {
        return serviceRepository.count();
    }
}
