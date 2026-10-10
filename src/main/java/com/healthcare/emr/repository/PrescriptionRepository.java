package com.healthcare.emr.repository;

import com.healthcare.emr.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByPatient_PatientIdOrderByPrescribedDateDesc(Long patientId);

    List<Prescription> findByDoctor_DoctorIdOrderByPrescribedDateDesc(Long doctorId);

    List<Prescription> findByMedicalRecord_RecordId(Long recordId);
}
