package com.healthcare.emr.repository;

import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUser(User user);
    Optional<Doctor> findByUser_UserId(Long userId);

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.department) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Doctor> searchDoctors(@Param("query") String query);
}
