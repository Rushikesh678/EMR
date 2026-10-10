package com.healthcare.emr.service;

import com.healthcare.emr.dto.UserDto;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final ReceptionStaffRepository receptionStaffRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                       DoctorRepository doctorRepository,
                       ReceptionStaffRepository receptionStaffRepository,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.receptionStaffRepository = receptionStaffRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto toggleUserStatus(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setActive(!user.isActive());
        User updated = userRepository.save(user);

        auditLogService.log(adminEmail, "ROLE_ADMIN", "TOGGLE_USER_STATUS", "User", userId, null,
                "Changed user status to " + (updated.isActive() ? "ACTIVE" : "INACTIVE"));

        return mapToDto(updated);
    }

    @Transactional
    public UserDto createStaffMember(Map<String, Object> payload, String adminEmail) {
        String roleStr = (String) payload.get("role");
        String email = ((String) payload.get("email")).toLowerCase().trim();
        String name = (String) payload.get("name");
        String password = (String) payload.get("password");
        String phone = (String) payload.get("phone");

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already in use: " + email);
        }

        Role role = Role.valueOf(roleStr);
        User user = new User(name, email, passwordEncoder.encode(password != null ? password : "Staff@123"), phone, role);
        User savedUser = userRepository.save(user);

        if (role == Role.ROLE_DOCTOR) {
            Doctor doctor = new Doctor();
            doctor.setUser(savedUser);
            doctor.setSpecialization((String) payload.getOrDefault("specialization", "General Medicine"));
            doctor.setQualification((String) payload.getOrDefault("qualification", "MBBS"));
            doctor.setDepartment((String) payload.getOrDefault("department", "Outpatient"));
            doctor.setExperienceYears(Integer.parseInt(payload.getOrDefault("experienceYears", "5").toString()));
            doctor.setConsultationFee(new BigDecimal(payload.getOrDefault("consultationFee", "60.00").toString()));
            doctorRepository.save(doctor);
        } else if (role == Role.ROLE_RECEPTIONIST) {
            ReceptionStaff staff = new ReceptionStaff();
            staff.setUser(savedUser);
            staff.setDepartment((String) payload.getOrDefault("department", "Admissions"));
            staff.setShift((String) payload.getOrDefault("shift", "Morning"));
            receptionStaffRepository.save(staff);
        }

        auditLogService.log(adminEmail, "ROLE_ADMIN", "CREATE_STAFF", "User", savedUser.getUserId(), null, "Created new " + role.name());

        return mapToDto(savedUser);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
