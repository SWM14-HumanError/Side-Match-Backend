package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.mentoring.ApplyMentoringRequest;
import com.example.matchup.matchupbackend.dto.response.mentoring.MentoringApplyListResponse;
import com.example.matchup.matchupbackend.dto.response.mentoring.TeamInfoResponse;
import com.example.matchup.matchupbackend.error.ErrorCode;
import com.example.matchup.matchupbackend.error.exception.ResourceNotPermitEx.ResourceNotPermitException;
import com.example.matchup.matchupbackend.service.MentoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(controllers = MentoringApplyController.class)
@WithMockUser
class MentoringApplyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MentoringService mentoringService;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("멘토링 신청 입력 폼 조회 성공")
    void getApplyMentoringInputForm_success() throws Exception {
        Mockito.when(mentoringService.getApplyMentoringInputForm(anyString()))
                .thenReturn(Collections.singletonList(Mockito.mock(TeamInfoResponse.class)));

        mockMvc.perform(get("/api/v1/mentoring/apply")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("멘토링 신청 성공")
    void postApplyMentoringByLeader_success() throws Exception {
        String requestJson = "{"
                + "\"teamId\": 1,"
                + "\"phoneNumber\": \"010-1234-5678\","
                + "\"email\": \"test@example.com\","
                + "\"content\": \"신청합니다.\""
                + "}";

        mockMvc.perform(post("/api/v1/mentoring/1/apply")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토가 신청 수락 성공")
    void postAcceptApplyMentoringByMentor_success() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/apply/1/accept")
                        .header("Authorization", AUTH_HEADER)
                        .param("comment", "수락합니다."))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토가 신청 수락 실패 - 코멘트 20자 초과")
    void postAcceptApplyMentoringByMentor_commentTooLong() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/apply/1/accept")
                        .header("Authorization", AUTH_HEADER)
                        .param("comment", "이 코멘트는 20자를 넘는 아주 긴 코멘트입니다."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("멘토가 신청 거절 성공")
    void postRefuseApplyMentoringByMentor_success() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/apply/1/refuse")
                        .header("Authorization", AUTH_HEADER)
                        .param("comment", "거절합니다."))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토가 신청 거절 실패 - 코멘트 20자 초과")
    void postRefuseApplyMentoringByMentor_commentTooLong() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/apply/1/refuse")
                        .header("Authorization", AUTH_HEADER)
                        .param("comment", "이 코멘트는 20자를 넘는 아주 긴 코멘트입니다."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("멘토링 신청 리스트 조회 성공")
    void getApplyMentoringList_success() throws Exception {
        Mockito.when(mentoringService.showApplyMentoringList(anyString()))
                .thenReturn(Collections.singletonList(Mockito.mock(MentoringApplyListResponse.class)));

        mockMvc.perform(get("/api/v1/mentoring/apply/list")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("멘토링 종료 성공")
    void postEndMentoringByMentor_success() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/1/done")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated());
    }
}