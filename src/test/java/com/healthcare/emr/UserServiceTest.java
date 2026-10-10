package com.healthcare.emr;

import com.healthcare.emr.dto.UserDto;
import com.healthcare.emr.entity.User;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.repository.UserRepository;
import com.healthcare.emr.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should retrieve users and toggle active status")
    void testGetAllUsersAndToggleStatus() {
        List<UserDto> users = userService.getAllUsers();
        assertFalse(users.isEmpty());

        User testUser = userRepository.findAll().get(0);
        boolean initialStatus = testUser.isActive();

        UserDto toggled = userService.toggleUserStatus(testUser.getUserId(), "admin@healthcare.local");
        assertEquals(!initialStatus, toggled.isActive());

        // Toggle back
        UserDto restored = userService.toggleUserStatus(testUser.getUserId(), "admin@healthcare.local");
        assertEquals(initialStatus, restored.isActive());
    }

    @Test
    @DisplayName("Should create doctor and receptionist staff members and reject duplicate emails")
    void testCreateStaffMembers() {
        // Create Doctor staff
        Map<String, Object> docPayload = new HashMap<>();
        docPayload.put("name", "Dr. Gregory House");
        docPayload.put("email", "dr.house." + System.currentTimeMillis() + "@healthcare.local");
        docPayload.put("password", "Pass@123");
        docPayload.put("role", "ROLE_DOCTOR");
        docPayload.put("phone", "+1-555-8822");
        docPayload.put("specialization", "Diagnostics");
        docPayload.put("qualification", "MD");
        docPayload.put("department", "Nephrology");
        docPayload.put("experienceYears", 15);
        docPayload.put("consultationFee", "150.00");

        UserDto docCreated = userService.createStaffMember(docPayload, "admin@healthcare.local");
        assertNotNull(docCreated);
        assertEquals("ROLE_DOCTOR", docCreated.getRole().name());

        // Create Receptionist staff
        Map<String, Object> staffPayload = new HashMap<>();
        staffPayload.put("name", "Pam Beesly");
        staffPayload.put("email", "pam.staff." + System.currentTimeMillis() + "@healthcare.local");
        staffPayload.put("password", "Pass@123");
        staffPayload.put("role", "ROLE_RECEPTIONIST");
        staffPayload.put("phone", "+1-555-4433");
        staffPayload.put("department", "Front Desk");
        staffPayload.put("shift", "Evening");

        UserDto staffCreated = userService.createStaffMember(staffPayload, "admin@healthcare.local");
        assertNotNull(staffCreated);
        assertEquals("ROLE_RECEPTIONIST", staffCreated.getRole().name());

        // Duplicate email rejection
        assertThrows(BadRequestException.class, () -> {
            userService.createStaffMember(docPayload, "admin@healthcare.local");
        });
    }
}
