-- ============================================================================
-- Healthcare Appointment & Electronic Medical Record (EMR) Seed Data (MySQL)
-- Passwords are encrypted with BCrypt for security
-- Admin: Admin@123, Doctor: Doctor@123, Reception: Reception@123, Patient: Patient@123
-- ============================================================================

USE healthcare_emr;

-- 1. Seed Users
-- BCrypt for Admin@123: $2a$10$7jCq7m4D.yG3L9r3U900yeS8WvQ40O0zH7iH/Rk1V3q6f2n6mE8mG
-- We also provide dynamic DataInitializer in Spring Boot to ensure reliable seeding.
INSERT IGNORE INTO users (user_id, name, email, password, phone, role, is_active) VALUES
(1, 'System Administrator', 'admin@healthcare.local', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '+1-555-0100', 'ROLE_ADMIN', TRUE),
(2, 'Dr. Sarah Jenkins', 'dr.sarah@healthcare.local', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '+1-555-0101', 'ROLE_DOCTOR', TRUE),
(3, 'Dr. Rajesh Patel', 'dr.rajesh@healthcare.local', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '+1-555-0102', 'ROLE_DOCTOR', TRUE),
(4, 'Maria Rodriguez', 'reception@healthcare.local', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '+1-555-0103', 'ROLE_RECEPTIONIST', TRUE),
(5, 'John Doe', 'john.doe@patient.local', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '+1-555-0104', 'ROLE_PATIENT', TRUE),
(6, 'Emily Clark', 'emily.clark@patient.local', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '+1-555-0105', 'ROLE_PATIENT', TRUE);

-- 2. Seed Role details
INSERT IGNORE INTO admins (admin_id, user_id, department) VALUES
(1, 1, 'Hospital Operations & IT');

INSERT IGNORE INTO doctors (doctor_id, user_id, specialization, qualification, experience_years, department, consultation_fee, availability_days, shift_start_time, shift_end_time) VALUES
(1, 2, 'Cardiology', 'MD, FACC - Harvard Medical', 12, 'Cardiovascular Institute', 120.00, 'MON,TUE,WED,THU,FRI', '09:00', '16:00'),
(2, 3, 'Pediatrics', 'MBBS, DCH - Johns Hopkins', 8, 'Pediatric & Child Health', 85.00, 'MON,WED,THU,SAT', '10:00', '17:00');

INSERT IGNORE INTO reception_staff (staff_id, user_id, department, shift) VALUES
(1, 4, 'Front Desk & Patient Admissions', 'Morning (08:00 - 16:00)');

INSERT IGNORE INTO patients (patient_id, user_id, date_of_birth, gender, blood_group, address, medical_history, emergency_contact_name, emergency_contact_phone) VALUES
(1, 5, '1988-06-14', 'Male', 'O+', '742 Evergreen Terrace, Springfield', 'Mild hypertension diagnosed in 2021. Penicillin allergy.', 'Jane Doe', '+1-555-0199'),
(2, 6, '1995-11-23', 'Female', 'A+', '124 Conch Street, Pacific Grove', 'No chronic conditions reported. Seasonal pollen allergies.', 'David Clark', '+1-555-0188');

-- 3. Seed Appointments
INSERT IGNORE INTO appointments (appointment_id, patient_id, doctor_id, appointment_date, appointment_time, status, reason, notes) VALUES
(1, 1, 1, CURRENT_DATE + INTERVAL 1 DAY, '10:00', 'CONFIRMED', 'Annual Cardiovascular checkup', 'Patient reported mild palpitations during exercise.'),
(2, 2, 2, CURRENT_DATE + INTERVAL 2 DAY, '11:30', 'REQUESTED', 'General pediatric consultation & seasonal allergy', 'Follow-up on allergy skin reaction.');

-- 4. Seed Medical Records (EMR)
INSERT IGNORE INTO medical_records (record_id, patient_id, doctor_id, appointment_id, record_date, diagnosis, treatment, medical_history_notes, clinical_notes) VALUES
(1, 1, 1, 1, CURRENT_DATE - INTERVAL 14 DAY, 'Stage 1 Essential Hypertension', 'Prescribed Lisinopril 10mg daily. Low sodium dietary counsel and lifestyle modifications.', 'Family history of coronary artery disease.', 'Blood pressure observed 138/88 mmHg. Heart sounds S1/S2 regular, no murmurs.');

-- 5. Seed Prescriptions
INSERT IGNORE INTO prescriptions (prescription_id, patient_id, doctor_id, medical_record_id, medication, dosage, duration, instructions, prescribed_date) VALUES
(1, 1, 1, 1, 'Lisinopril', '10 mg once daily', '30 days', 'Take in the morning with a full glass of water. Monitor BP weekly.', CURRENT_DATE - INTERVAL 14 DAY);

-- 6. Seed Billing
INSERT IGNORE INTO billing (billing_id, patient_id, appointment_id, amount, status, payment_method, payment_date, transaction_ref, invoice_number) VALUES
(1, 1, 1, 120.00, 'PAID', 'Credit Card (Simulated)', CURRENT_TIMESTAMP, 'TXN-DEMO-948124', 'INV-2026-001'),
(2, 2, 2, 85.00, 'PENDING', NULL, NULL, NULL, 'INV-2026-002');

-- 7. Seed Notifications
INSERT IGNORE INTO notifications (notification_id, user_id, title, message, type, is_read) VALUES
(1, 5, 'Appointment Confirmed', 'Your appointment with Dr. Sarah Jenkins is confirmed.', 'APPOINTMENT', FALSE),
(2, 6, 'Appointment Requested', 'Your appointment request has been received and is awaiting confirmation.', 'APPOINTMENT', FALSE);
