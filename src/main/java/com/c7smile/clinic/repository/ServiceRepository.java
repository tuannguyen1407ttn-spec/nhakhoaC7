package com.c7smile.clinic.repository;

import com.c7smile.clinic.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    Optional<Service> findBySlug(String slug);
    List<Service> findTop4ByOrderByIdAsc();
}
