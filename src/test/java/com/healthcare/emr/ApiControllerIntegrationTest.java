package com.healthcare.emr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.emr.dto.AuthRequest;
import com.healthcare.emr.entity.Doctor;
import com.healthcare.emr.repository.DoctorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("GET /api/doctors should return public doctors directory wrapped in ApiResponse")
    void testPublicDoctorsEndpoint() throws Exception {
        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].doctorId").exists());
    }

    @Test
    @DisplayName("GET /api/doctors/{id} should return specific doctor details")
    void testDoctorDetailsEndpoint() throws Exception {
        Doctor doc = doctorRepository.findAll().get(0);
        mockMvc.perform(get("/api/doctors/" + doc.getDoctorId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(doc.getUser().getName()))
                .andExpect(jsonPath("$.data.specialization").value(doc.getSpecialization()));
    }

    @Test
    @DisplayName("GET /api/doctors/{id}/available-slots should return available schedule slots")
    void testDoctorAvailableSlotsEndpoint() throws Exception {
        Doctor doc = doctorRepository.findAll().get(0);
        mockMvc.perform(get("/api/doctors/" + doc.getDoctorId() + "/available-slots")
                        .param("date", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slots").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/login with valid admin credentials should return JWT token in ApiResponse")
    void testLoginEndpoint() throws Exception {
        AuthRequest loginRequest = new AuthRequest("admin@healthcare.local", "Admin@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid password should return 401 Unauthorized")
    void testLoginBadCredentials() throws Exception {
        AuthRequest loginRequest = new AuthRequest("admin@healthcare.local", "WrongPass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@healthcare.local", authorities = {"ROLE_ADMIN"})
    @DisplayName("GET /api/reports/analytics with ADMIN authority should return system analytics")
    void testAdminReportsEndpoint() throws Exception {
        mockMvc.perform(get("/api/reports/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPatients").isNumber());
    }
}
