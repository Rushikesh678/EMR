package com.healthcare.emr.repository;

import com.healthcare.emr.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUser_UserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUser_UserIdAndReadFalse(Long userId);
}
