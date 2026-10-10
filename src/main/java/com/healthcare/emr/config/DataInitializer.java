package com.healthcare.emr.config;

import com.healthcare.emr.entity.*;
import com.healthcare.emr.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ReceptionStaffRepository receptionStaffRepository;
    private final AdminRepository adminRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final BillingRepository billingRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           ReceptionStaffRepository receptionStaffRepository,
                           AdminRepository adminRepository,
                           AppointmentRepository appointmentRepository,
                           MedicalRecordRepository medicalRecordRepository,
                           PrescriptionRepository prescriptionRepository,
                           BillingRepository billingRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.receptionStaffRepository = receptionStaffRepository;
        this.adminRepository = adminRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.billingRepository = billingRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already populated with {} users. Skipping initialization.", userRepository.count());
            return;
        }

        log.info("Seeding initial fictional demo healthcare data...");

        // 1. Admin
        User adminUser = new User("System Administrator", "admin@healthcare.local", passwordEncoder.encode("Admin@123"), "+1-555-0100", Role.ROLE_ADMIN);
        userRepository.save(adminUser);
        Admin admin = new Admin(adminUser, "Hospital Operations & IT");
        adminRepository.save(admin);

        // 2. Doctor 1 (Cardiology)
        User docUser1 = new User("Dr. Sarah Jenkins", "dr.sarah@healthcare.local", passwordEncoder.encode("Doctor@123"), "+1-555-0101", Role.ROLE_DOCTOR);
        userRepository.save(docUser1);
        Doctor doc1 = new Doctor(docUser1, "Cardiology", "MD, FACC - Harvard Medical", 12, "Cardiovascular Institute", new BigDecimal("120.00"));
        doc1.setAvailabilityDays("MON,TUE,WED,THU,FRI");
        doc1.setShiftStartTime("09:00");
        doc1.setShiftEndTime("16:00");
        doctorRepository.save(doc1);

        // 3. Doctor 2 (Pediatrics)
        User docUser2 = new User("Dr. Rajesh Patel", "dr.rajesh@healthcare.local", passwordEncoder.encode("Doctor@123"), "+1-555-0102", Role.ROLE_DOCTOR);
        userRepository.save(docUser2);
        Doctor doc2 = new Doctor(docUser2, "Pediatrics", "MBBS, DCH - Johns Hopkins", 8, "Pediatric & Child Health", new BigDecimal("85.00"));
        doc2.setAvailabilityDays("MON,WED,THU,SAT");
        doc2.setShiftStartTime("10:00");
        doc2.setShiftEndTime("17:00");
        doctorRepository.save(doc2);

        // 4. Receptionist
        User recUser = new User("Maria Rodriguez", "reception@healthcare.local", passwordEncoder.encode("Reception@123"), "+1-555-0103", Role.ROLE_RECEPTIONIST);
        userRepository.save(recUser);
        ReceptionStaff recStaff = new ReceptionStaff(recUser, "Front Desk & Patient Admissions", "Morning (08:00 - 16:00)");
        receptionStaffRepository.save(recStaff);

        // 5. Patient 1
        User ptUser1 = new User("John Doe", "john.doe@patient.local", passwordEncoder.encode("Patient@123"), "+1-555-0104", Role.ROLE_PATIENT);
        userRepository.save(ptUser1);
        Patient patient1 = new Patient(ptUser1, LocalDate.of(1988, 6, 14), "Male", "O+", "742 Evergreen Terrace, Springfield", "Mild hypertension diagnosed in 2021. Penicillin allergy.");
        patient1.setEmergencyContactName("Jane Doe");
        patient1.setEmergencyContactPhone("+1-555-0199");
        patientRepository.save(patient1);

        // 6. Patient 2
        User ptUser2 = new User("Emily Clark", "emily.clark@patient.local", passwordEncoder.encode("Patient@123"), "+1-555-0105", Role.ROLE_PATIENT);
        userRepository.save(ptUser2);
        Patient patient2 = new Patient(ptUser2, LocalDate.of(1995, 11, 23), "Female", "A+", "124 Conch Street, Pacific Grove", "Seasonal pollen allergies.");
        patient2.setEmergencyContactName("David Clark");
        patient2.setEmergencyContactPhone("+1-555-0188");
        patientRepository.save(patient2);

        // 7. Sample Appointments
        Appointment appt1 = new Appointment(patient1, doc1, LocalDate.now().plusDays(1), "10:00", AppointmentStatus.CONFIRMED, "Annual Cardiovascular checkup");
        appt1.setNotes("Patient reported mild palpitations during exercise.");
        appointmentRepository.save(appt1);

        Appointment appt2 = new Appointment(patient2, doc2, LocalDate.now().plusDays(2), "11:30", AppointmentStatus.REQUESTED, "General pediatric consultation & allergy check");
        appointmentRepository.save(appt2);

        // 8. Sample Clinical EMR Record
        MedicalRecord record1 = new MedicalRecord(
                patient1,
                doc1,
                appt1,
                LocalDate.now().minusDays(14),
                "Stage 1 Essential Hypertension",
                "Prescribed Lisinopril 10mg daily. Advised low sodium dietary counsel and lifestyle modifications.",
                "BP 138/88 mmHg. Heart sounds S1/S2 regular, no murmurs. Patient counseled on regular exercise."
        );
        record1.setMedicalHistoryNotes("Family history of coronary artery disease.");
        medicalRecordRepository.save(record1);

        // 9. Sample Prescription
        Prescription rx1 = new Prescription(
                patient1,
                doc1,
                record1,
                "Lisinopril",
                "10 mg once daily",
                "30 days",
                "Take in the morning with water. Monitor blood pressure weekly.",
                LocalDate.now().minusDays(14)
        );
        prescriptionRepository.save(rx1);

        // 10. Sample Billing Records
        Billing bill1 = new Billing(patient1, appt1, new BigDecimal("120.00"), BillingStatus.PAID, "INV-2026-001");
        bill1.setPaymentMethod("Credit Card (Simulated)");
        bill1.setPaymentDate(LocalDateTime.now().minusDays(1));
        bill1.setTransactionRef("TXN-DEMO-948124");
        billingRepository.save(bill1);

        Billing bill2 = new Billing(patient2, appt2, new BigDecimal("85.00"), BillingStatus.PENDING, "INV-2026-002");
        billingRepository.save(bill2);

        // 11. Initial Notifications
        Notification n1 = new Notification(ptUser1, "Appointment Confirmed", "Your appointment with Dr. Sarah Jenkins is confirmed for tomorrow at 10:00.", "APPOINTMENT");
        notificationRepository.save(n1);

        Notification n2 = new Notification(ptUser2, "Welcome to Healthcare Portal", "Welcome Emily! You can book appointments and view prescriptions anytime.", "INFO");
        notificationRepository.save(n2);

        log.info("Demo fictional healthcare data initialized successfully!");
    }
}
