package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.mentoring.CreateOrEditMentoringRequest;
import com.example.matchup.matchupbackend.dto.request.mentoring.MentoringSearchParam;
import com.example.matchup.matchupbackend.dto.request.mentoring.ReviewMentoringRequest;
import com.example.matchup.matchupbackend.dto.response.mentoring.MentoringSearchResponse;
import com.example.matchup.matchupbackend.dto.response.mentoring.MentoringSliceResponse;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(controllers = MentoringController.class)
@WithMockUser
class MentoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MentoringService mentoringService;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("멘토링 목록 조회 성공")
    void getMentoring_success() throws Exception {
        Mockito.when(mentoringService.showMentoringsInMentoringPage(any(), any(), any(Pageable.class)))
                .thenReturn(Mockito.mock(MentoringSliceResponse.class));

        mockMvc.perform(get("/api/v1/mentorings")
                        .header("Authorization", AUTH_HEADER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("멘토링 생성 성공")
    void postMentoring_success() throws Exception {
        String requestJson = "{"
                + "\"title\": \"멘토링 제목\","
                + "\"content\": \"멘토링 상세 내용입니다.\","
                + "\"stacks\": [\"Java\", \"Spring\", \"React\"],"
                + "\"roleType\": \"BE\","
                + "\"career\": \"주니어\","
                + "\"imageName\": \"profile.png\","
                + "\"imageBase64\": \"base64encodedstring\""
                + "}";
        Mockito.when(mentoringService.createMentoringByMentor(anyString(), any(CreateOrEditMentoringRequest.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/mentoring")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토링 수정 성공")
    void putMentoring_success() throws Exception {
        String requestJson = "{"
                + "\"title\": \"멘토링 제목\","
                + "\"content\": \"멘토링 상세 내용입니다.\","
                + "\"stacks\": [\"Java\", \"Spring\", \"React\"],"
                + "\"roleType\": \"BE\","
                + "\"career\": \"주니어\","
                + "\"imageName\": \"profile.png\","
                + "\"imageBase64\": \"base64encodedstring\""
                + "}";
        Mockito.when(mentoringService.editMentoringByMentor(anyString(), any(CreateOrEditMentoringRequest.class), anyLong()))
                .thenReturn(1L);

        mockMvc.perform(put("/api/v1/mentoring/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("멘토링 삭제 성공")
    void deleteMentoring_success() throws Exception {
        Mockito.when(mentoringService.deleteMentoringByMentor(anyString(), anyLong()))
                .thenReturn(1L);

        mockMvc.perform(delete("/api/v1/mentoring/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("멘토링 상세 조회 성공")
    void getMentoringDetail_success() throws Exception {
        Mockito.when(mentoringService.showMentoringDetail(anyLong()))
                .thenReturn(Mockito.mock(MentoringSearchResponse.class));

        mockMvc.perform(get("/api/v1/mentoring/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("멘토링 좋아요 성공")
    void postMentoringLike_success() throws Exception {
        mockMvc.perform(post("/api/v1/mentoring/1/like")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토링 좋아요 취소 성공")
    void deleteMentoringLike_success() throws Exception {
        mockMvc.perform(delete("/api/v1/mentoring/1/like")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("멘토링 리뷰 등록 성공")
    void postMentoringReview_success() throws Exception {
        String requestJson = "{"
                + "\"satisfaction\": 5,"
                + "\"expertise\": 4,"
                + "\"punctuality\": 5,"
                + "\"comment\": \"정말 좋았어요\""
                + "}";

        mockMvc.perform(post("/api/v1/mentoring/1/review/2")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("멘토 페이지에서 진행중 멘토링 조회 성공")
    void getMentoringActiveOnMentorPage_success() throws Exception {
        Mockito.when(mentoringService.showActiveMentoringOnMentorPage(anyString()))
                .thenReturn(Collections.singletonList(Mockito.mock(MentoringSearchResponse.class)));

        mockMvc.perform(get("/api/v1/mentor/active")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("좋아요한 멘토링 목록 조회 성공")
    void getMentoringLiked_success() throws Exception {
        Mockito.when(mentoringService.showMentoringLiked(anyString()))
                .thenReturn(Collections.singletonList(Mockito.mock(MentoringSearchResponse.class)));

        mockMvc.perform(get("/api/v1/mentoring/likes")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내가 등록한 멘토링 목록 조회 성공")
    void getMentoringMine_success() throws Exception {
        Mockito.when(mentoringService.showMentoringMine(anyString()))
                .thenReturn(Collections.singletonList(Mockito.mock(MentoringSearchResponse.class)));

        mockMvc.perform(get("/api/v1/mentoring/mine")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }
}