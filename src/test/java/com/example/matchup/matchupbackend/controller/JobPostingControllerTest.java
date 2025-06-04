package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.jobposting.JobPostingRequest;
import com.example.matchup.matchupbackend.dto.request.jobposting.JobPostingSearchRequest;
import com.example.matchup.matchupbackend.dto.response.jobposting.JobPostingDetailResponse;
import com.example.matchup.matchupbackend.dto.response.jobposting.JobPostingPageResponse;
import com.example.matchup.matchupbackend.entity.JobPosting;
import com.example.matchup.matchupbackend.entity.Role;
import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.JobPostingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(controllers = JobPostingController.class)
@WithMockUser
class JobPostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private JobPostingService jobPostingService;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("채용 공고 목록 조회 성공")
    void searchJobPosting_success() throws Exception {
        Mockito.when(jobPostingService.searchJobPosting(any(JobPostingSearchRequest.class), any(Pageable.class)))
                .thenReturn(Mockito.mock(JobPostingPageResponse.class));

        mockMvc.perform(get("/api/v1/job-posting")
                        .param("keyword", "백엔드")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("채용 공고 등록 성공(관리자)")
    void createJobPosting_success() throws Exception {
        User admin = Mockito.mock(User.class);
        Mockito.when(admin.getRole()).thenReturn(Role.ADMIN);
        Mockito.when(tokenProvider.getUser(anyString(), anyString())).thenReturn(admin);

        JobPosting jobPosting = Mockito.mock(JobPosting.class);
        Mockito.when(jobPosting.getId()).thenReturn(1L);
        Mockito.when(jobPostingService.saveJobPosting(any(JobPostingRequest.class))).thenReturn(jobPosting);

        String requestJson = "{ \"title\": \"채용 제목\", \"content\": \"채용 내용\" }";

        mockMvc.perform(post("/api/v1/job-posting")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().string("채용 공고 ID : 1"));
    }

    @Test
    @DisplayName("채용 공고 등록 실패(관리자 아님)")
    void createJobPosting_forbidden() throws Exception {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getRole()).thenReturn(Role.USER);
        Mockito.when(tokenProvider.getUser(anyString(), anyString())).thenReturn(user);

        String requestJson = "{ \"title\": \"채용 제목\", \"content\": \"채용 내용\" }";

        mockMvc.perform(post("/api/v1/job-posting")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("채용 공고 상세 조회 성공")
    void getDetailJobPosting_success() throws Exception {
        Mockito.when(jobPostingService.getDetailJobPosting(anyLong()))
                .thenReturn(Mockito.mock(JobPostingDetailResponse.class));

        mockMvc.perform(get("/api/v1/job-posting/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("채용 공고 삭제 성공(관리자)")
    void deleteJobPosting_success() throws Exception {
        User admin = Mockito.mock(User.class);
        Mockito.when(admin.getRole()).thenReturn(Role.ADMIN);
        Mockito.when(tokenProvider.getUser(anyString(), anyString())).thenReturn(admin);

        Mockito.when(jobPostingService.deleteJobPosting(anyLong())).thenReturn("삭제 완료");

        mockMvc.perform(delete("/api/v1/job-posting/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(content().string("삭제 완료"));
    }

    @Test
    @DisplayName("채용 공고 삭제 실패(관리자 아님)")
    void deleteJobPosting_forbidden() throws Exception {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getRole()).thenReturn(Role.USER);
        Mockito.when(tokenProvider.getUser(anyString(), anyString())).thenReturn(user);

        mockMvc.perform(delete("/api/v1/job-posting/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isUnauthorized());
    }
}