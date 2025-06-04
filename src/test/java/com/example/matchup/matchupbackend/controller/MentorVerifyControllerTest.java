package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.mentoring.ApplyVerifyMentorRequest;
import com.example.matchup.matchupbackend.dto.response.mentoring.VerifyMentorsResponse;
import com.example.matchup.matchupbackend.dto.response.mentoring.VerifyMentorsSliceResponse;
import com.example.matchup.matchupbackend.service.MentoringService;
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
@WebMvcTest(controllers = MentorVerifyController.class)
@WithMockUser
class MentorVerifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MentoringService mentoringService;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("멘토 인증 신청 성공")
    void postVerifyMentor_success() throws Exception {
        String requestJson = "{ \"field1\": \"value1\", \"field2\": \"value2\" }"; // 실제 필드에 맞게 수정
        mockMvc.perform(post("/api/v1/mentoring/verify")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토 인증 목록 조회 성공")
    void getVerifyMentors_success() throws Exception {
        Mockito.when(mentoringService.showVerifyMentors(anyString(), any(Pageable.class)))
                .thenReturn(Mockito.mock(VerifyMentorsSliceResponse.class));

        mockMvc.perform(get("/api/v1/mentoring/verify/list")
                        .header("Authorization", AUTH_HEADER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("멘토 인증 승인 성공")
    void postAcceptVerifyMentors_success() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/verify/1/accept")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토 인증 거절 성공")
    void postRefuseVerifyMentors_success() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/verify/1/refuse")
                        .header("Authorization", AUTH_HEADER)
                        .param("comment", "사유"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토 인증 거절 - 20자 초과시 예외")
    void postRefuseVerifyMentors_fail_commentTooLong() throws Exception {
        String longComment = "a".repeat(21);
        mockMvc.perform(post("/api/v1/mentoring/verify/1/refuse")
                        .header("Authorization", AUTH_HEADER)
                        .param("comment", longComment))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("멘토 인증 수정 폼 조회 성공")
    void getVerifyMentorEditForm_success() throws Exception {
        Mockito.when(mentoringService.showVerifyMentorEditForm(anyString()))
                .thenReturn(Mockito.mock(VerifyMentorsResponse.class));

        mockMvc.perform(get("/api/v1/mentoring/verify")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("멘토 인증 수정 성공")
    void putVerifyMentor_success() throws Exception {
        String requestJson = "{ \"field1\": \"value1\", \"field2\": \"value2\" }"; // 실제 필드에 맞게 수정
        mockMvc.perform(put("/api/v1/mentoring/verify")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());
    }
}