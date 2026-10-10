package com.healthcare.emr.repository;

import com.healthcare.emr.entity.ReceptionStaff;
import com.healthcare.emr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceptionStaffRepository extends JpaRepository<ReceptionStaff, Long> {
    Optional<ReceptionStaff> findByUser(User user);
    Optional<ReceptionStaff> findByUser_UserId(Long userId);
}
