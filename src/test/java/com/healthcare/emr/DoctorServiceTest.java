package com.healthcare.emr;

import com.healthcare.emr.dto.DoctorDto;
import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.DoctorRepository;
import com.healthcare.emr.service.DoctorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DoctorServiceTest {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("Should fetch all doctors and search by name and specialty")
    void testGetAllAndSearchDoctors() {
        List<DoctorDto> doctors = doctorService.getAllDoctors();
        assertFalse(doctors.isEmpty(), "Seeded doctors should not be empty");

        Doctor firstDoc = doctorRepository.findAll().get(0);

        // Search by doctor name
        List<DoctorDto> nameResults = doctorService.searchDoctors(firstDoc.getUser().getName().substring(0, 4));
        assertFalse(nameResults.isEmpty());

        // Search by specialization
        List<DoctorDto> specResults = doctorService.searchDoctors(firstDoc.getSpecialization());
        assertFalse(specResults.isEmpty());

        // Empty search returns all
        List<DoctorDto> allFromEmptySearch = doctorService.searchDoctors("");
        assertEquals(doctors.size(), allFromEmptySearch.size());
    }

    @Test
    @DisplayName("Should fetch doctor by ID and by User ID")
    void testGetDoctorByIdAndUserId() {
        Doctor firstDoc = doctorRepository.findAll().get(0);

        DoctorDto byId = doctorService.getDoctorById(firstDoc.getDoctorId());
        assertNotNull(byId);
        assertEquals(firstDoc.getDoctorId(), byId.getDoctorId());
        assertEquals(firstDoc.getUser().getName(), byId.getName());

        DoctorDto byUserId = doctorService.getDoctorByUserId(firstDoc.getUser().getUserId());
        assertNotNull(byUserId);
        assertEquals(firstDoc.getDoctorId(), byUserId.getDoctorId());

        assertThrows(ResourceNotFoundException.class, () -> doctorService.getDoctorById(999999L));
        assertThrows(ResourceNotFoundException.class, () -> doctorService.getDoctorByUserId(999999L));
    }

    @Test
    @DisplayName("Should calculate available slots on working days and return unavailable on off days")
    void testGetAvailableSlots() {
        Doctor doc = doctorRepository.findAll().get(0);

        // Next Monday (working day)
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Map<String, Object> slotsMon = doctorService.getAvailableSlots(doc.getDoctorId(), nextMonday);

        assertNotNull(slotsMon);
        assertTrue((Boolean) slotsMon.get("isWorkingDay"));
        List<?> slotsList = (List<?>) slotsMon.get("slots");
        assertFalse(slotsList.isEmpty(), "Working day should have available slots");

        // Next Sunday (non-working day for default MON,TUE,WED,THU,FRI)
        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        Map<String, Object> slotsSun = doctorService.getAvailableSlots(doc.getDoctorId(), nextSunday);

        assertNotNull(slotsSun);
        assertFalse((Boolean) slotsSun.get("isWorkingDay"));
    }
}
