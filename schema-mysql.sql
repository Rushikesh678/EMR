-- ============================================================================
-- Healthcare Appointment & Electronic Medical Record (EMR) System Schema (MySQL)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS healthcare_emr;
USE healthcare_emr;

-- 1. Users table (Parent identity table)
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(25),
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
);

-- 2. Patients table (Role-specific child entity)
CREATE TABLE IF NOT EXISTS patients (
    patient_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    address VARCHAR(255),
    medical_history TEXT,
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(25),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_patient_user (user_id)
);

-- 3. Doctors table (Role-specific child entity)
CREATE TABLE IF NOT EXISTS doctors (
    doctor_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    specialization VARCHAR(100) NOT NULL,
    qualification VARCHAR(100),
    experience_years INT DEFAULT 0,
    department VARCHAR(100),
    consultation_fee DECIMAL(10,2) DEFAULT 50.00,
    availability_days VARCHAR(100) DEFAULT 'MON,TUE,WED,THU,FRI',
    shift_start_time VARCHAR(10) DEFAULT '09:00',
    shift_end_time VARCHAR(10) DEFAULT '17:00',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_doctor_specialization (specialization)
);

-- 4. Reception Staff table
CREATE TABLE IF NOT EXISTS reception_staff (
    staff_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department VARCHAR(100),
    shift VARCHAR(50) DEFAULT 'Morning',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 5. Administrators table
CREATE TABLE IF NOT EXISTS admins (
    admin_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department VARCHAR(100) DEFAULT 'System Administration',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 6. Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    CONSTRAINT uk_doctor_slot UNIQUE (doctor_id, appointment_date, appointment_time),
    INDEX idx_appt_date (appointment_date),
    INDEX idx_appt_patient (patient_id),
    INDEX idx_appt_doctor (doctor_id)
);

-- 7. Medical Records (EMR)
CREATE TABLE IF NOT EXISTS medical_records (
    record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_id BIGINT,
    record_date DATE NOT NULL,
    diagnosis VARCHAR(255) NOT NULL,
    treatment TEXT,
    medical_history_notes TEXT,
    clinical_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    INDEX idx_emr_patient (patient_id)
);

-- 8. Prescriptions table
CREATE TABLE IF NOT EXISTS prescriptions (
    prescription_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    medical_record_id BIGINT,
    medication VARCHAR(255) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,
    instructions TEXT,
    prescribed_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    FOREIGN KEY (medical_record_id) REFERENCES medical_records(record_id),
    INDEX idx_rx_patient (patient_id)
);

-- 9. Laboratory Reports table
CREATE TABLE IF NOT EXISTS laboratory_reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT,
    medical_record_id BIGINT,
    test_name VARCHAR(150) NOT NULL,
    test_date DATE NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    result_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id),
    FOREIGN KEY (medical_record_id) REFERENCES medical_records(record_id),
    INDEX idx_lab_patient (patient_id)
);

-- 10. Billing and Payments table
CREATE TABLE IF NOT EXISTS billing (
    billing_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    payment_date TIMESTAMP NULL,
    transaction_ref VARCHAR(100),
    invoice_number VARCHAR(50) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    INDEX idx_bill_patient (patient_id)
);

-- 11. Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id)
);

-- 12. Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(120),
    user_role VARCHAR(30),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    ip_address VARCHAR(50),
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_timestamp (timestamp)
);
