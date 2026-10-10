package com.healthcare.emr.service;

import com.healthcare.emr.dto.DashboardSummaryDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final BillingRepository billingRepository;

    public ReportService(UserRepository userRepository,
                         PatientRepository patientRepository,
                         DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         MedicalRecordRepository medicalRecordRepository,
                         PrescriptionRepository prescriptionRepository,
                         BillingRepository billingRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.billingRepository = billingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSystemAnalytics() {
        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setTotalPatients(patientRepository.count());
        dto.setTotalDoctors(doctorRepository.count());
        dto.setTotalAppointments(appointmentRepository.count());
        dto.setAppointmentsToday(appointmentRepository.countByAppointmentDate(LocalDate.now()));
        dto.setPendingAppointments(appointmentRepository.countByStatus(AppointmentStatus.REQUESTED));
        dto.setCompletedAppointments(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED));
        dto.setTotalPrescriptions(prescriptionRepository.count());
        dto.setTotalMedicalRecords(medicalRecordRepository.count());

        BigDecimal paid = billingRepository.sumTotalPaidRevenue();
        dto.setTotalPaidRevenue(paid != null ? paid : BigDecimal.ZERO);

        BigDecimal pending = billingRepository.sumTotalPendingRevenue();
        dto.setTotalPendingRevenue(pending != null ? pending : BigDecimal.ZERO);

        // Grouping appointments by status
        Map<String, Long> statusMap = new HashMap<>();
        for (AppointmentStatus st : AppointmentStatus.values()) {
            statusMap.put(st.name(), appointmentRepository.countByStatus(st));
        }
        dto.setAppointmentsByStatus(statusMap);

        // Grouping appointments by doctor specialization
        Map<String, Long> specMap = new HashMap<>();
        List<Appointment> allAppts = appointmentRepository.findAll();
        for (Appointment a : allAppts) {
            String spec = a.getDoctor().getSpecialization();
            specMap.put(spec, specMap.getOrDefault(spec, 0L) + 1);
        }
        dto.setAppointmentsBySpecialization(specMap);

        return dto;
    }

    public byte[] generateSystemReportPdf() {
        DashboardSummaryDto stats = getSystemAnalytics();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Font
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(15, 76, 129));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 41, 59));
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Title
            Paragraph title = new Paragraph("Healthcare Appointment & EMR System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph("Comprehensive Executive Operations & Analytics Report\nGenerated: "
                    + LocalDate.now().format(DateTimeFormatter.ISO_DATE), subTitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            document.add(sub);

            // Key Metrics Table
            Paragraph s1 = new Paragraph("1. Key Operational Metrics", sectionFont);
            s1.setSpacingAfter(8);
            document.add(s1);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            addTableRow(table, "Total Registered Patients", String.valueOf(stats.getTotalPatients()));
            addTableRow(table, "Total Active Doctors", String.valueOf(stats.getTotalDoctors()));
            addTableRow(table, "Total System Appointments", String.valueOf(stats.getTotalAppointments()));
            addTableRow(table, "Appointments Scheduled Today", String.valueOf(stats.getAppointmentsToday()));
            addTableRow(table, "Completed Clinical Consultations", String.valueOf(stats.getCompletedAppointments()));
            addTableRow(table, "Total EMR Medical Records Created", String.valueOf(stats.getTotalMedicalRecords()));
            addTableRow(table, "Total Prescriptions Issued", String.valueOf(stats.getTotalPrescriptions()));
            addTableRow(table, "Total Revenue Collected (Paid)", "$" + stats.getTotalPaidRevenue());
            addTableRow(table, "Pending Patient Invoices", "$" + stats.getTotalPendingRevenue());
            document.add(table);

            // Specialization Breakdown
            Paragraph s2 = new Paragraph("2. Appointment Volume by Specialization", sectionFont);
            s2.setSpacingAfter(8);
            document.add(s2);

            PdfPTable specTable = new PdfPTable(2);
            specTable.setWidthPercentage(100);
            specTable.setSpacingAfter(15);
            addHeaderCell(specTable, "Specialization / Department", tableHeaderFont);
            addHeaderCell(specTable, "Appointments Count", tableHeaderFont);

            for (Map.Entry<String, Long> entry : stats.getAppointmentsBySpecialization().entrySet()) {
                specTable.addCell(new Phrase(entry.getKey(), textFont));
                specTable.addCell(new Phrase(String.valueOf(entry.getValue()), textFont));
            }
            document.add(specTable);

            // Doctor Performance Summary
            Paragraph s3 = new Paragraph("3. Medical Staff Directory & Status", sectionFont);
            s3.setSpacingAfter(8);
            document.add(s3);

            PdfPTable docTable = new PdfPTable(4);
            docTable.setWidthPercentage(100);
            addHeaderCell(docTable, "Doctor Name", tableHeaderFont);
            addHeaderCell(docTable, "Specialty", tableHeaderFont);
            addHeaderCell(docTable, "Experience", tableHeaderFont);
            addHeaderCell(docTable, "Fee", tableHeaderFont);

            List<Doctor> doctors = doctorRepository.findAll();
            for (Doctor d : doctors) {
                docTable.addCell(new Phrase(d.getUser().getName(), textFont));
                docTable.addCell(new Phrase(d.getSpecialization(), textFont));
                docTable.addCell(new Phrase(d.getExperienceYears() + " yrs", textFont));
                docTable.addCell(new Phrase("$" + d.getConsultationFee(), textFont));
            }
            document.add(docTable);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error rendering PDF report", e);
        }

        return out.toByteArray();
    }

    public byte[] generatePrescriptionPdf(Long prescriptionId) {
        Prescription rx = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));

        Document document = new Document(PageSize.A5, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font clinicFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(15, 76, 129));
            Font docFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font rxSymbolFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(15, 76, 129));

            Paragraph header = new Paragraph("HEALTHCARE CLINICAL SERVICES", clinicFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph docInfo = new Paragraph("Prescribing Doctor: " + rx.getDoctor().getUser().getName()
                    + "\nSpecialty: " + rx.getDoctor().getSpecialization()
                    + "\nContact: " + rx.getDoctor().getUser().getPhone(), docFont);
            docInfo.setSpacingAfter(15);
            document.add(docInfo);

            Paragraph line = new Paragraph("--------------------------------------------------------------------------------");
            document.add(line);

            Paragraph ptInfo = new Paragraph("Patient: " + rx.getPatient().getUser().getName()
                    + "   |   Date: " + rx.getPrescribedDate()
                    + "\nPrescription ID: RX-" + rx.getPrescriptionId(), valueFont);
            ptInfo.setSpacingAfter(15);
            document.add(ptInfo);

            Paragraph rxSymbol = new Paragraph("℞", rxSymbolFont);
            document.add(rxSymbol);

            PdfPTable medTable = new PdfPTable(2);
            medTable.setWidthPercentage(100);
            medTable.setSpacingBefore(10);
            medTable.setSpacingAfter(15);

            addTableRow(medTable, "Medication", rx.getMedication());
            addTableRow(medTable, "Dosage", rx.getDosage());
            addTableRow(medTable, "Duration", rx.getDuration());
            addTableRow(medTable, "Instructions", rx.getInstructions() != null ? rx.getInstructions() : "As directed");
            document.add(medTable);

            Paragraph footer = new Paragraph("\n\nDoctor's Signature: ______________________\n(Authorized Digital Medical Record)", labelFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error rendering prescription PDF", e);
        }

        return out.toByteArray();
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        Font lFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font vFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        PdfPCell c1 = new PdfPCell(new Phrase(label, lFont));
        PdfPCell c2 = new PdfPCell(new Phrase(value, vFont));
        c1.setPadding(6);
        c2.setPadding(6);
        table.addCell(c1);
        table.addCell(c2);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(15, 76, 129));
        cell.setPadding(6);
        table.addCell(cell);
    }
}
