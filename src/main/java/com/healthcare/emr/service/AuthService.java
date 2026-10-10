package com.healthcare.emr.service;

import com.healthcare.emr.dto.AuthRequest;
import com.healthcare.emr.dto.AuthResponse;
import com.healthcare.emr.dto.PasswordRecoveryRequest;
import com.healthcare.emr.dto.RegisterRequest;
import com.healthcare.emr.entity.*;
import com.healthcare.emr.exception.BadRequestException;
import com.healthcare.emr.exception.ResourceNotFoundException;
import com.healthcare.emr.repository.*;
import com.healthcare.emr.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ReceptionStaffRepository receptionStaffRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       ReceptionStaffRepository receptionStaffRepository,
                       AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       NotificationService notificationService,
                       AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.receptionStaffRepository = receptionStaffRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated. Please contact administration.");
        }

        String token = tokenProvider.generateToken(authentication);
        Long roleEntityId = resolveRoleEntityId(user);
        String redirectPath = resolveRedirectPath(user.getRole());

        auditLogService.log(user.getEmail(), user.getRole().name(), "LOGIN", "User", user.getUserId(), null, "Successful login");

        return new AuthResponse(token, user.getUserId(), user.getName(), user.getEmail(), user.getRole().name(), roleEntityId, redirectPath);
    }

    @Transactional
    public AuthResponse registerPatient(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.ROLE_PATIENT);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setAddress(request.getAddress());
        patient.setMedicalHistory(request.getMedicalHistory());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        Patient savedPatient = patientRepository.save(patient);

        notificationService.sendNotification(
                savedUser,
                "Welcome to Healthcare System",
                "Your account has been registered successfully. You can now book appointments and view your health records.",
                "WELCOME"
        );

        auditLogService.log(savedUser.getEmail(), Role.ROLE_PATIENT.name(), "REGISTER", "Patient", savedPatient.getPatientId(), null, "Self registered new patient");

        String token = tokenProvider.generateTokenFromEmail(savedUser.getEmail());
        return new AuthResponse(token, savedUser.getUserId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().name(), savedPatient.getPatientId(), resolveRedirectPath(Role.ROLE_PATIENT));
    }

    public void initiatePasswordRecovery(PasswordRecoveryRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        String mockResetToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        notificationService.sendEmail(
                user.getEmail(),
                "Password Recovery Verification Code",
                "Your password reset verification code is: " + mockResetToken + ". Enter this code or contact clinic support to reset your credentials."
        );
    }

    @Transactional
    public void resetPassword(PasswordRecoveryRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        notificationService.sendNotification(user, "Password Changed", "Your account password was updated successfully.", "SECURITY");
        auditLogService.log(user.getEmail(), user.getRole().name(), "PASSWORD_RESET", "User", user.getUserId(), null, "Password reset successfully");
    }

    public Long resolveRoleEntityId(User user) {
        switch (user.getRole()) {
            case ROLE_PATIENT:
                return patientRepository.findByUser(user).map(Patient::getPatientId).orElse(null);
            case ROLE_DOCTOR:
                return doctorRepository.findByUser(user).map(Doctor::getDoctorId).orElse(null);
            case ROLE_RECEPTIONIST:
                return receptionStaffRepository.findByUser(user).map(ReceptionStaff::getStaffId).orElse(null);
            case ROLE_ADMIN:
                return adminRepository.findByUser(user).map(Admin::getAdminId).orElse(null);
            default:
                return null;
        }
    }

    public String resolveRedirectPath(Role role) {
        switch (role) {
            case ROLE_PATIENT:
                return "#patient-portal";
            case ROLE_DOCTOR:
                return "#doctor-portal";
            case ROLE_RECEPTIONIST:
                return "#reception-portal";
            case ROLE_ADMIN:
                return "#admin-portal";
            default:
                return "#";
        }
    }
}
