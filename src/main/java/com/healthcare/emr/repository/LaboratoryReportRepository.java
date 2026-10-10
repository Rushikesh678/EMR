package com.healthcare.emr.repository;

import com.healthcare.emr.entity.LaboratoryReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LaboratoryReportRepository extends JpaRepository<LaboratoryReport, Long> {

    List<LaboratoryReport> findByPatient_PatientIdOrderByTestDateDesc(Long patientId);

    List<LaboratoryReport> findByDoctor_DoctorIdOrderByTestDateDesc(Long doctorId);

    List<LaboratoryReport> findByMedicalRecord_RecordId(Long recordId);
}
