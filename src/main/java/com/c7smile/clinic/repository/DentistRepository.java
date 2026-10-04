package com.c7smile.clinic.repository;

import com.c7smile.clinic.entity.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DentistRepository extends JpaRepository<Dentist, Long> {
    Optional<Dentist> findBySlug(String slug);
}
