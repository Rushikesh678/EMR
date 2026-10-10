# Healthcare Appointment & Electronic Medical Record (EMR) System

**Project type:** College Software Engineering project\
**Purpose:** Build a web application for patient appointments,
electronic medical records, prescriptions, billing, reports, and
administration.

> This README is the product specification to give Claude before it
> generates the website. It describes intended functionality, not
> features that have already been implemented.

## 1. Instructions for Claude

Treat this document as the source of truth.

1.  Inspect the existing repository before editing. Preserve working
    code; if the repository is empty, establish the structure first.
2.  Build a working application, not just a static UI. Forms,
    navigation, validation, role-based dashboards, and core workflows
    must work end to end.
3.  Use the stack below unless the existing repository already has a
    functioning stack. Do not silently replace Spring Boot or MySQL.
4.  Work incrementally. Implement one coherent layer at a time, run
    tests, and explain changed files and how to verify them.
5.  Never hardcode secrets, passwords, API keys, or database
    credentials. Provide configuration templates with placeholders.
6.  Enforce permissions on the backend; hiding frontend buttons is not
    sufficient security.
7.  Use fictional demo data only. Do not use real patient information.
8.  Do not claim integrations work unless implemented and tested. Use a
    clearly labelled demo payment flow until a real provider is
    configured.
9.  Include setup instructions, database setup, tests, and known
    limitations.
10. Keep the code understandable for a college student: clear naming,
    sensible folders, useful comments, and no unnecessary complexity.

## 2. Project Overview

The Healthcare Appointment & Electronic Medical Record (EMR) System is a
web-based application for hospitals or clinics. It centralizes patient
registration, appointments, doctor schedules, medical records,
prescriptions, laboratory reports, billing, and administrative
reporting. Its goals are to reduce paperwork, organize healthcare
information, avoid appointment conflicts, and provide controlled access
to sensitive records.

## 3. User Roles and Permissions

### Patient

-   Register, log in, log out, and update their own profile.
-   Book appointments with available doctors.
-   View appointment status and appointment history.
-   Cancel or reschedule eligible appointments.
-   View only their own authorized medical records and history.
-   View/download their prescriptions.
-   View bills and payment status.
-   View relevant notifications.

### Doctor

-   View appointments and schedules.
-   View information and history for patients they are authorized to
    treat.
-   Create/update authorized medical records.
-   Record diagnoses and treatment details.
-   Create/update prescriptions, including medication, dosage, duration,
    and instructions.
-   Upload or associate laboratory reports and medical documents where
    supported.

### Reception Staff

-   Register patients and update patient information.
-   Search patient profiles.
-   Schedule, update, cancel, and reschedule appointments.
-   Check doctor availability and manage scheduling information
    according to permissions.
-   View relevant appointment lists and authorized reports.
-   Do not grant unrestricted access to clinical notes or prescription
    editing.

### Administrator

-   Manage users and roles; activate/deactivate accounts.
-   Manage doctor schedules.
-   View system summaries and generate reports.
-   Manage billing records and system settings.
-   Monitor system activity and audit events.
-   Restrict access to sensitive medical information to what is
    necessary and authorized.

A user must only access functions and records allowed for their role.

## 4. Technology Stack

Keep the implementation consistent with the project's SRS: -
**Frontend:** HTML5, CSS3, JavaScript. - **Backend:** Java 17 or later,
Spring Boot. - **Database:** MySQL. - **Build tool:** Maven. - **API:**
REST endpoints returning JSON. - **Version control:** Git.

Separate frontend assets, controllers, services, repositories, domain
entities, DTOs, security, configuration, and exception handling. If the
repository already uses a different frontend framework, inspect it and
do not rewrite it without a clear reason.

## 5. Main Modules

### Authentication and User Management

-   Registration, login, logout, password recovery flow.
-   Role-based access control for Patient, Doctor, Reception Staff, and
    Administrator.
-   Account activation/deactivation and profile management.
-   Secure password hashing; never store plain-text passwords.
-   Session/token expiry and server-side validation.

### Patient Management

-   Create/update profiles, search and view authorized patient
    information.
-   Maintain emergency contact information.
-   Assign each patient a unique Patient ID.
-   Avoid permanently deleting clinical records. Prefer deactivation or
    archival where appropriate.

### Appointment Management

-   Book, view, search, update, cancel, and reschedule appointments.
-   Display available doctors and time slots.
-   Check availability before confirming a booking.
-   Prevent conflicting appointments for the same doctor and time slot.
-   Generate a unique Appointment ID.
-   Store date, time, patient, doctor, reason, and status.
-   Show confirmation and appointment history.
-   Keep appointment changes consistent in the database.

Suggested statuses: `REQUESTED`, `CONFIRMED`, `CANCELLED`, `COMPLETED`,
`RESCHEDULED`. Use consistent status values throughout the app.

### EMR Management

-   Create, view, and update authorized patient medical records.
-   Store diagnosis, treatment, medical history, clinical notes, record
    date, and responsible doctor.
-   Search records only within the user's authorization.
-   Generate a readable medical summary.
-   Audit important record changes.
-   Prevent unauthorized access and modification.
-   Do not permanently delete created medical records; use controlled
    correction or archival if needed.

### Prescription Management

-   Create prescriptions for registered patients.
-   Link each prescription to its patient and prescribing doctor.
-   Store medication, dosage, duration, instructions, and date.
-   View/update prescriptions subject to authorization.
-   Let the relevant patient view or download their prescription.
-   Validate required fields before saving.

### Laboratory Reports and Medical Documents

-   Upload reports and relevant medical documents.
-   Associate each report with a registered patient and, where
    appropriate, a medical record.
-   Validate file type and size; do not trust user-supplied filenames or
    paths.
-   Store files safely and restrict access to authorized users.
-   Provide protected viewing/download; do not expose private files
    through guessable public URLs.

### Billing and Payments

-   Generate bills associated with a patient and, where applicable, an
    appointment.
-   Store amount, date, payment method, and payment status.
-   Let authorized users view relevant billing information.
-   Implement a demo/simulated payment flow unless real payment-provider
    credentials and integration are supplied.
-   Never store card numbers, CVVs, or payment secrets.

### Reports and Analytics

Support: - Appointment report. - Patient report. - Doctor performance
report. - Prescription report. - Monthly statistics. - Daily appointment
summary. - Patient visit analysis. - PDF export.

Respect role permissions and avoid exposing unnecessary patient details.
Do not show fake success for unimplemented PDF export.

### Email Notifications

Plan for appointment confirmations, reminders, password recovery, and
relevant notifications. If no provider credentials are available,
implement a development mode that previews or logs messages and label it
clearly.

## 6. Class Model Reference

The existing class diagram contains these 12 classes. Use them as the
initial domain-model reference; refine only when needed for a sound
implementation.

### User

Attributes: `userId`, `name`, `email`, `password`, `phone`, `role`,
`isActive`\
Methods: `login()`, `logout()`, `updateProfile()`

### Patient

Attributes: `patientId`, `dateOfBirth`, `gender`, `address`,
`medicalHistory`\
Methods: `bookAppointment()`, `cancelAppointment()`,
`viewMedicalRecords()`, `makePayment()`

### Doctor

Attributes: `doctorId`, `specialization`, `qualification`, `experience`,
`availability`\
Methods: `viewPatientDetails()`, `updateEMR()`, `addPrescription()`,
`viewSchedule()`

### ReceptionStaff

Attributes: `staffId`, `department`, `shift`\
Methods: `registerPatient()`, `manageAppointments()`,
`updatePatientInfo()`, `viewReports()`

### Admin

Attributes: `adminId`, `department`\
Methods: `manageUsers()`, `viewReports()`, `manageBilling()`,
`configureSystem()`

### Appointment

Attributes: `appointmentId`, `patientId`, `doctorId`, `date`, `time`,
`status`, `reason`\
Methods: `schedule()`, `reschedule()`, `cancel()`, `getDetails()`

### EMR

Attributes: `emrId`, `patientId`, `doctorId`, `diagnosis`,
`prescription`, `medicalHistory`, `labReports`\
Methods: `addRecord()`, `updateRecord()`, `getRecords()`

### MedicalRecord

Attributes: `recordId`, `patientId`, `doctorId`, `date`, `notes`\
Methods: `addRecord()`, `updateRecord()`, `getRecord()`

### Prescription

Attributes: `prescriptionId`, `patientId`, `doctorId`, `medication`,
`dosage`, `instructions`, `date`\
Methods: `addPrescription()`, `updatePrescription()`,
`getPrescription()`

### Billing

Attributes: `billingId`, `patientId`, `appointmentId`, `amount`,
`status`, `paymentMethod`, `date`\
Methods: `generateBill()`, `makePayment()`, `getBillingDetails()`

### Report

Attributes: `reportId`, `type`, `generatedDate`, `data`\
Methods: `generateReport()`, `getReport()`, `export()`

### EmailNotificationService

Attributes: `serviceId`, `provider`, `status`\
Methods: `sendEmail()`, `getStatus()`

### Relationships

-   `User` is the parent/generalized class for `Patient`, `Doctor`,
    `ReceptionStaff`, and `Admin`.
-   A patient can have multiple appointments; a doctor can be assigned
    multiple appointments.
-   Appointments connect patients and doctors.
-   Patients have medical records; doctors create/update authorized
    records and prescriptions.
-   Prescriptions are linked to patients and doctors and may be
    associated with a medical record.
-   Billing records are associated with patients and appointments.
-   Administrators manage users, billing, and system configuration.
-   Reports are generated from appropriate system data.
-   EmailNotificationService sends configured notifications.

**Database note:** The class diagram is a design reference, not a
requirement to store every relationship as a plain string. Use primary
keys, foreign keys, and proper relational associations. Avoid
duplicating the same user identity across unrelated tables.

## 7. Suggested Website Pages

### Public pages

-   Home/landing page.
-   About or services overview.
-   Login.
-   Patient registration.
-   Password recovery.

### Patient dashboard

-   Overview and upcoming appointments.
-   Book appointment.
-   My appointments/history.
-   My medical records.
-   My prescriptions.
-   My bills/payment status.
-   Profile and notifications.

### Doctor dashboard

-   Today's appointments and schedule.
-   Authorized patient list/search.
-   Patient medical history.
-   Create/update medical record.
-   Create prescription.
-   Laboratory reports/documents.
-   Profile and notifications.

### Reception dashboard

-   Register/search/update patient.
-   Book and manage appointments.
-   Doctor availability and schedules.
-   Daily appointment list.

### Administrator dashboard

-   System overview.
-   User management.
-   Doctor schedules.
-   Appointment overview.
-   Billing management.
-   Reports and PDF export.
-   System settings and audit/activity log.

Redirect users to the correct dashboard after login. Do not show every
dashboard or action to every role.

## 8. UI and UX Requirements

-   Professional, modern, trustworthy healthcare design.
-   Calm visual palette, consistent typography, spacing, icons, and
    reusable components.
-   Responsive desktop, tablet, and mobile layouts.
-   Clear role-specific navigation.
-   Accessible labels, keyboard-friendly forms, and adequate contrast.
-   Inline validation and helpful error messages.
-   Confirmation before cancelling appointments or performing sensitive
    actions.
-   Loading indicators, empty states, and success/error feedback.
-   Search, filters, and pagination for large lists where appropriate.
-   Fictional demo data only.
-   Avoid clutter, excessive animations, and unnecessary decoration.

## 9. Suggested Database Entities

Use MySQL with primary keys, foreign keys, constraints, indexes, and
timestamps. Final schema may add tables as required.

Starting entities: - `users` - `patients` - `doctors` -
`reception_staff` (if role-specific fields need a separate table) -
`appointments` - `medical_records` - `prescriptions` -
`laboratory_reports` - `billing` - `notifications` - `audit_logs` -
`reports` (if report metadata needs persistence)

A common `users` table should hold login credentials and role;
patient/doctor/staff tables can hold role-specific details. Provide
migrations or a schema script so the database can be recreated.

## 10. Backend Structure and API

Suggested Spring Boot packages: - `controller` --- REST endpoints. -
`service` --- business logic. - `repository` --- database access. -
`entity` / `model` --- persistence entities. - `dto` ---
request/response objects. - `security` --- authentication and
authorization. - `exception` --- consistent error handling. - `config`
--- configuration.

Use appropriate HTTP methods/status codes, server-side validation, and
safe API responses. Never return passwords, secret fields, or
unnecessary sensitive information.

Suggested API groups: - `/api/auth` - `/api/users` - `/api/patients` -
`/api/doctors` - `/api/appointments` - `/api/medical-records` -
`/api/prescriptions` - `/api/laboratory-reports` - `/api/billing` -
`/api/reports` - `/api/notifications`

These are proposed route groups, not existing endpoints. Document actual
endpoints as they are implemented.

## 11. Security and Privacy

This application handles sensitive health information. Privacy and
authorization are core requirements.

-   Hash passwords using a recognized password-hashing mechanism.
-   Enforce role-based access on the backend.
-   Patients can access only their own records.
-   Doctors can access only patients/records they are authorized to
    treat.
-   Reception staff get only the access needed for registration and
    scheduling.
-   Validate and sanitize incoming data.
-   Use HTTPS in production.
-   Protect sessions/tokens and apply expiry.
-   Protect against SQL injection, cross-site scripting, CSRF where
    relevant, and unsafe uploads.
-   Keep secrets out of Git.
-   Audit important actions, especially changes to records,
    prescriptions, roles, and billing.
-   Do not log passwords, tokens, or full sensitive medical details.
-   Use least-privilege database credentials and regular backups.
-   Do not claim legal or healthcare compliance unless assessed and
    implemented.

## 12. Performance and Reliability Targets

These are project targets that must be tested, not assumed: - Login
response within 2 seconds. - Appointment search within 3 seconds. - EMR
retrieval within 3 seconds. - Up to 500 concurrent users. - At least 99%
availability. - Automatic daily database backup.

## 13. Testing Requirements

Test at minimum: - Registration, login, logout, and invalid
credentials. - Backend role-based authorization for protected pages and
APIs. - Patients cannot access another patient's records. - Doctors
cannot update unauthorized records. - Booking, availability,
cancellation, and rescheduling. - Prevention of conflicting
appointments. - Medical-record create/update permissions. - Prescription
creation and patient access. - File upload validation and protected
downloads. - Billing status and demo-payment behavior. - Report
generation/export. - Email development mode and provider errors. -
Responsive layout, form validation, and API/database failures.

Add unit tests for important business logic and integration tests for
key API/database workflows where feasible.

## 14. Local Setup and Configuration

Document actual commands after implementation. Prerequisites: - Java 17
or later. - Maven. - MySQL Server. - Git. - Modern web browser.

Provide: 1. Clone instructions. 2. Database creation and
migration/schema instructions. 3. Database configuration through
environment variables or a local untracked configuration file. 4.
Backend build/run commands. 5. Frontend run instructions if a separate
development server is used. 6. Test commands. 7. Demo account
instructions if demo users are seeded. 8. Troubleshooting for database,
port, and configuration issues.

Never put real credentials in this README. Add `.gitignore` entries for
build output, IDE files, logs, local configuration, `.env` files, and
private uploaded documents where appropriate.

## 15. Incremental Implementation Plan

### Phase 1 --- Foundation

Inspect repository, set up Spring Boot/Maven, frontend assets, MySQL
connection, schema/migrations, configuration templates, and error
handling.

### Phase 2 --- Authentication and Roles

Registration/login/logout, password hashing, role-based access, and
role-specific dashboard routing.

### Phase 3 --- Patient and Appointment Management

Profiles/search, doctor schedules/availability, booking, confirmation,
cancellation, and rescheduling.

### Phase 4 --- EMR and Prescriptions

Medical records/history, diagnosis/treatment, prescriptions, and
laboratory document uploads.

### Phase 5 --- Billing, Reports, and Notifications

Billing records, clearly labelled demo payments, reports/PDF export, and
email integration or development preview mode.

### Phase 6 --- Quality and Delivery

Privacy and authorization tests, responsive UI, accessibility, error
handling, backup documentation, and complete setup instructions.

## 16. Definition of Done

The first demonstrable release is ready when: - The app starts locally
using documented instructions. - MySQL persistence works. - Login and
role-based authorization work on both frontend and backend. - All four
roles have the correct dashboards and permissions. - Patient,
appointment, EMR, and prescription workflows work end to end. -
Validation and errors are handled properly. - No secrets or real patient
information are committed. - Core workflows have tests or documented
manual test steps. - Unimplemented integrations are clearly marked as
pending/demo. - Setup, configuration, and known limitations are
documented.

## 17. Future Enhancements

Potential future integrations: - Real payment gateway. - SMS
notifications. - Pharmacy systems. - Laboratory systems. - Health
insurance providers. - Cloud backup. - Multi-language support.

Treat these as future enhancements unless implemented and tested.

------------------------------------------------------------------------

## Final instruction to Claude

Read this entire README before coding. First inspect the current
repository and report what already exists. Then provide a short, ordered
implementation plan and begin with **Phase 1**. Preserve the
requirements and class model, implement one coherent layer at a time,
run relevant tests, and explain exactly which files changed and how to
verify them. Do not generate a frontend-only prototype while claiming
the complete system is functional.
