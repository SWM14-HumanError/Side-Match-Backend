package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.teamuser.*;
import com.example.matchup.matchupbackend.dto.response.teamuser.*;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.TeamUserService;
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
@WithMockUser
@WebMvcTest(TeamUserController.class)
class TeamUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TeamUserService teamUserService;

    @MockBean
    TokenProvider tokenProvider;

    private final String token = "Bearer test.jwt.token";

    @Test
    @DisplayName("팀 멤버 목록 조회 성공")
    void showTeamUsers_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(teamUserService.getTeamUserCard(anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/team/1/member")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 모집 정보 조회 성공")
    void showTeamRecruit_success() throws Exception {
        Mockito.when(teamUserService.getTeamApprovedMemberInfo(anyLong()))
                .thenReturn(Mockito.mock(TeamApprovedInfoResponse.class));

        mockMvc.perform(get("/api/v1/team/1/recruitInfo"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 지원 성공")
    void recruitToTeam_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(teamUserService.recruitToTeam(anyLong(), anyLong(), any(RecruitFormRequest.class)))
                .thenReturn(100L);
        String requestJson = "{"
                + "\"role\":\"FE\","
                + "\"content\":\"프론트엔드 개발자로 지원합니다.\""
                + "}";

        mockMvc.perform(post("/api/v1/team/1/recruit")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("팀원 수락 성공")
    void acceptUserToTeam_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        String requestJson = "{"
                + "\"recruitUserID\":2,"
                + "\"role\":\"FE\""
                + "}";
        mockMvc.perform(post("/api/v1/team/1/acceptUser")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("팀원 거절 성공")
    void refuseUserToTeam_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        String requestJson = "{"
                + "\"recruitUserID\":2,"
                + "\"refuseReason\":\"프로젝트와 맞지 않아 거절합니다.\""
                + "}";
        mockMvc.perform(delete("/api/v1/team/1/refuseUser")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("팀원 강퇴 성공")
    void kickUserToTeam_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);

        String requestJson = "{"
                + "\"kickUserID\":3,"
                + "\"role\":\"FE\","
                + "\"refuseReason\":\"팀 규칙 위반으로 인한 방출입니다.\""
                + "}";

        mockMvc.perform(delete("/api/v1/team/1/kickUser")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("팀원 피드백 성공")
    void feedbackEachTeamUser_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);

        String requestJson = "{"
                + "\"receiverID\":2,"
                + "\"grade\":\"GREAT\","
                + "\"isContactable\":true,"
                + "\"isOnTime\":false,"
                + "\"isResponsible\":true,"
                + "\"isKind\":true,"
                + "\"isCollaboration\":true,"
                + "\"isFast\":false,"
                + "\"isActively\":true,"
                + "\"commentToUser\":\"정말 좋은 팀원이었습니다.\","
                + "\"commentToAdmin\":\"추가 코멘트가 있다면 여기에 작성\""
                + "}";

        mockMvc.perform(post("/api/v1/team/1/feedback")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("지원서 열람 성공")
    void showRecruit_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(teamUserService.getRecruitInfo(anyLong(), anyLong(), anyLong()))
                .thenReturn(Mockito.mock(RecruitInfoResponse.class));

        mockMvc.perform(get("/api/v1/team/1/recruit/1")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("거절 사유 열람 성공")
    void showRefuseReason_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(teamUserService.getUserRefuseReason(anyLong(), anyLong()))
                .thenReturn(Mockito.mock(RefuseReasonResponse.class));

        mockMvc.perform(get("/api/v1/team/refuse/1")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }
}