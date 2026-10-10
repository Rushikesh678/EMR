package com.healthcare.emr.repository;

import com.healthcare.emr.entity.Appointment;
import com.healthcare.emr.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatient_PatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    List<Appointment> findByDoctor_DoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(Long doctorId);

    List<Appointment> findByDoctor_DoctorIdAndAppointmentDateOrderByAppointmentTimeAsc(Long doctorId, LocalDate appointmentDate);

    List<Appointment> findByAppointmentDateOrderByAppointmentTimeAsc(LocalDate appointmentDate);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE " +
           "a.doctor.doctorId = :doctorId AND " +
           "a.appointmentDate = :appointmentDate AND " +
           "a.appointmentTime = :appointmentTime AND " +
           "a.status <> 'CANCELLED'")
    boolean existsConflictingAppointment(
            @Param("doctorId") Long doctorId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("appointmentTime") String appointmentTime);

    long countByStatus(AppointmentStatus status);

    long countByAppointmentDate(LocalDate date);
}
