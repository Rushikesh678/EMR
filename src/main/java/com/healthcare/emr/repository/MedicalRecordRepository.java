package com.healthcare.emr.repository;

import com.healthcare.emr.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findByPatient_PatientIdOrderByRecordDateDesc(Long patientId);

    List<MedicalRecord> findByDoctor_DoctorIdOrderByRecordDateDesc(Long doctorId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.patientId = :patientId AND " +
           "(LOWER(m.diagnosis) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.treatment) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.clinicalNotes) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<MedicalRecord> searchPatientRecords(@Param("patientId") Long patientId, @Param("query") String query);
}
