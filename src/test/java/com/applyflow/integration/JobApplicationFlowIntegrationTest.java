package com.applyflow.integration;

import com.applyflow.dto.JobApplicationRequest;
import com.applyflow.enums.ApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobApplicationFlowIntegrationTest extends BaseIntegrationTest {

        private JobApplicationRequest createSampleRequest() {
                return JobApplicationRequest.builder()
                                .companyName("Google")
                                .position("Software Engineer")
                                .status(ApplicationStatus.APPLIED)
                                .applicationDate(LocalDate.now())
                                .salaryExpectation(new BigDecimal("120000.00"))
                                .contactPerson("Jane HR")
                                .notes("Applied via LinkedIn")
                                .build();
        }

        @Test
        @DisplayName("Should create a job application")
        void createApplication_Success() throws Exception {
                String token = registerAndGetToken();
                JobApplicationRequest request = createSampleRequest();

                mockMvc.perform(post(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.companyName").value("Google"))
                                .andExpect(jsonPath("$.position").value("Software Engineer"))
                                .andExpect(jsonPath("$.status").value("APPLIED"));
        }

        @Test
        @DisplayName("Should reject application with missing required fields")
        void createApplication_ValidationError() throws Exception {
                String token = registerAndGetToken();
                JobApplicationRequest request = JobApplicationRequest.builder().build();

                mockMvc.perform(post(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should retrieve application by ID")
        void getById_Success() throws Exception {
                String token = registerAndGetToken();
                Long id = createAndReturnId(token);

                mockMvc.perform(get(APPLICATIONS_URL + "/" + id)
                                .header("Authorization", authHeader(token)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id))
                                .andExpect(jsonPath("$.companyName").value("Google"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent application")
        void getById_NotFound() throws Exception {
                String token = registerAndGetToken();

                mockMvc.perform(get(APPLICATIONS_URL + "/999")
                                .header("Authorization", authHeader(token)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should list all applications with pagination")
        void getAll_WithPagination() throws Exception {
                String token = registerAndGetToken();

                for (int i = 0; i < 3; i++) {
                        JobApplicationRequest request = JobApplicationRequest.builder()
                                        .companyName("Company " + i)
                                        .position("Position " + i)
                                        .status(ApplicationStatus.APPLIED)
                                        .applicationDate(LocalDate.now())
                                        .build();

                        mockMvc.perform(post(APPLICATIONS_URL)
                                        .header("Authorization", authHeader(token))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated());
                }

                mockMvc.perform(get(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token))
                                .param("page", "0")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                .andExpect(jsonPath("$.totalElements").value(3))
                                .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("Should filter applications by status")
        void getAll_FilterByStatus() throws Exception {
                String token = registerAndGetToken();

                JobApplicationRequest applied = createSampleRequest();
                applied.setStatus(ApplicationStatus.APPLIED);
                mockMvc.perform(post(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(applied)))
                                .andExpect(status().isCreated());

                JobApplicationRequest interview = createSampleRequest();
                interview.setCompanyName("Meta");
                interview.setStatus(ApplicationStatus.INTERVIEW);
                mockMvc.perform(post(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(interview)))
                                .andExpect(status().isCreated());

                mockMvc.perform(get(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token))
                                .param("status", "INTERVIEW"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].companyName").value("Meta"));
        }

        @Test
        @DisplayName("Should update a job application")
        void updateApplication_Success() throws Exception {
                String token = registerAndGetToken();
                Long id = createAndReturnId(token);

                JobApplicationRequest updateRequest = createSampleRequest();
                updateRequest.setPosition("Senior Software Engineer");
                updateRequest.setStatus(ApplicationStatus.INTERVIEW);

                mockMvc.perform(put(APPLICATIONS_URL + "/" + id)
                                .header("Authorization", authHeader(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.position").value("Senior Software Engineer"))
                                .andExpect(jsonPath("$.status").value("INTERVIEW"));
        }

        @Test
        @DisplayName("Should delete a job application")
        void deleteApplication_Success() throws Exception {
                String token = registerAndGetToken();
                Long id = createAndReturnId(token);

                mockMvc.perform(delete(APPLICATIONS_URL + "/" + id)
                                .header("Authorization", authHeader(token)))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get(APPLICATIONS_URL + "/" + id)
                                .header("Authorization", authHeader(token)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should get status change history after update")
        void getStatusHistory_AfterUpdate() throws Exception {
                String token = registerAndGetToken();
                Long id = createAndReturnId(token);

                JobApplicationRequest updateRequest = createSampleRequest();
                updateRequest.setStatus(ApplicationStatus.INTERVIEW);
                mockMvc.perform(put(APPLICATIONS_URL + "/" + id)
                                .header("Authorization", authHeader(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk());

                mockMvc.perform(get(APPLICATIONS_URL + "/" + id + "/history")
                                .header("Authorization", authHeader(token)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].previousStatus").value("APPLIED"))
                                .andExpect(jsonPath("$[0].newStatus").value("INTERVIEW"));
        }

        @Test
        @DisplayName("Should prevent accessing another user's application")
        void ownershipValidation() throws Exception {
                String token1 = registerAndGetToken("User1", "user1@example.com", "password123");
                Long id = createAndReturnId(token1);

                String token2 = registerAndGetToken("User2", "user2@example.com", "password123");

                // Service returns 404 to hide existence of other users' resources
                mockMvc.perform(get(APPLICATIONS_URL + "/" + id)
                                .header("Authorization", authHeader(token2)))
                                .andExpect(status().isNotFound());
        }

        private Long createAndReturnId(String token) throws Exception {
                JobApplicationRequest request = createSampleRequest();

                MvcResult result = mockMvc.perform(post(APPLICATIONS_URL)
                                .header("Authorization", authHeader(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                return objectMapper.readTree(result.getResponse().getContentAsString())
                                .get("id").asLong();
        }
}
