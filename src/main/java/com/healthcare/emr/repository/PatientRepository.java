package com.healthcare.emr.repository;

import com.healthcare.emr.entity.Patient;
import com.healthcare.emr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUser(User user);
    Optional<Patient> findByUser_UserId(Long userId);

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.user.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.user.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Patient> searchPatients(@Param("query") String query);
}
