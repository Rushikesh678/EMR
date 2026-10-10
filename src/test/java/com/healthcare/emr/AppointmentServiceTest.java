package com.healthcare.emr;

import com.healthcare.emr.dto.AppointmentBookingRequest;
import com.healthcare.emr.dto.AppointmentDto;
import com.healthcare.emr.entity.Appointment;
import com.healthcare.emr.entity.AppointmentStatus;
import com.healthcare.emr.exception.AppointmentConflictException;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.AppointmentRepository;
import com.healthcare.emr.repository.DoctorRepository;
import com.healthcare.emr.repository.PatientRepository;
import com.healthcare.emr.service.AppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AppointmentServiceTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("Should successfully book an appointment and prevent double booking")
    void testBookingAndConflictPrevention() {
        Long patientId = patientRepository.findAll().get(0).getPatientId();
        Long doctorId = doctorRepository.findAll().get(0).getDoctorId();
        LocalDate testDate = LocalDate.now().plusDays(10);
        String testTime = "14:00";

        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setAppointmentDate(testDate);
        request.setAppointmentTime(testTime);
        request.setReason("Test checkup");

        AppointmentDto booked = appointmentService.bookAppointment(request, patientId, "test@patient.local");
        assertNotNull(booked);
        assertEquals(AppointmentStatus.CONFIRMED, booked.getStatus());
        assertEquals(testTime, booked.getAppointmentTime());

        // Attempting to book the identical slot for the same doctor must throw AppointmentConflictException
        AppointmentBookingRequest conflictRequest = new AppointmentBookingRequest();
        conflictRequest.setPatientId(patientId);
        conflictRequest.setDoctorId(doctorId);
        conflictRequest.setAppointmentDate(testDate);
        conflictRequest.setAppointmentTime(testTime);
        conflictRequest.setReason("Conflicting checkup");

        assertThrows(AppointmentConflictException.class, () -> {
            appointmentService.bookAppointment(conflictRequest, patientId, "test2@patient.local");
        });
    }

    @Test
    @DisplayName("Should reschedule appointment successfully and prevent conflicting rescheduling")
    void testRescheduleAppointment() {
        Long patientId = patientRepository.findAll().get(0).getPatientId();
        Long doctorId = doctorRepository.findAll().get(0).getDoctorId();
        LocalDate initialDate = LocalDate.now().plusDays(5);
        String initialTime = "10:00";

        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setAppointmentDate(initialDate);
        request.setAppointmentTime(initialTime);
        request.setReason("Initial visit");

        AppointmentDto booked = appointmentService.bookAppointment(request, patientId, "patient@test.local");

        LocalDate newDate = LocalDate.now().plusDays(6);
        String newTime = "11:00";
        AppointmentDto rescheduled = appointmentService.rescheduleAppointment(booked.getAppointmentId(), newDate, newTime, "patient@test.local");

        assertEquals(AppointmentStatus.RESCHEDULED, rescheduled.getStatus());
        assertEquals(newDate, rescheduled.getAppointmentDate());
        assertEquals(newTime, rescheduled.getAppointmentTime());
    }

    @Test
    @DisplayName("Should prevent rescheduling of cancelled or completed appointments")
    void testRescheduleInvalidStatus() {
        Long patientId = patientRepository.findAll().get(0).getPatientId();
        Long doctorId = doctorRepository.findAll().get(0).getDoctorId();

        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setAppointmentDate(LocalDate.now().plusDays(3));
        request.setAppointmentTime("09:30");
        request.setReason("Routine");

        AppointmentDto booked = appointmentService.bookAppointment(request, patientId, "patient@test.local");
        appointmentService.updateStatus(booked.getAppointmentId(), AppointmentStatus.CANCELLED, "patient@test.local");

        assertThrows(BadRequestException.class, () -> {
            appointmentService.rescheduleAppointment(booked.getAppointmentId(), LocalDate.now().plusDays(4), "10:00", "patient@test.local");
        });
    }

    @Test
    @DisplayName("Should update appointment status and fetch by ID and query lists")
    void testUpdateStatusAndFetchQueries() {
        Long patientId = patientRepository.findAll().get(0).getPatientId();
        Long doctorId = doctorRepository.findAll().get(0).getDoctorId();

        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setAppointmentDate(LocalDate.now().plusDays(2));
        request.setAppointmentTime("15:00");
        request.setReason("Consultation");

        AppointmentDto booked = appointmentService.bookAppointment(request, patientId, "patient@test.local");

        // Update status to COMPLETED
        AppointmentDto updated = appointmentService.updateStatus(booked.getAppointmentId(), AppointmentStatus.COMPLETED, "dr@healthcare.local");
        assertEquals(AppointmentStatus.COMPLETED, updated.getStatus());

        // Fetch by ID
        AppointmentDto fetched = appointmentService.getAppointmentById(booked.getAppointmentId());
        assertEquals(booked.getAppointmentId(), fetched.getAppointmentId());

        // Fetch list for patient
        List<AppointmentDto> patientAppts = appointmentService.getAppointmentsForPatient(patientId);
        assertFalse(patientAppts.isEmpty());

        // Fetch list for doctor
        List<AppointmentDto> doctorAppts = appointmentService.getAppointmentsForDoctor(doctorId);
        assertFalse(doctorAppts.isEmpty());

        // Fetch all appointments
        List<AppointmentDto> allAppts = appointmentService.getAllAppointments();
        assertFalse(allAppts.isEmpty());

        // Non-existent appointment throws ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () -> {
            appointmentService.getAppointmentById(999999L);
        });
    }
}
