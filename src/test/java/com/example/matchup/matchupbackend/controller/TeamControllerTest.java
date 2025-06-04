package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.team.TeamCreateRequest;
import com.example.matchup.matchupbackend.dto.request.team.TeamSearchRequest;
import com.example.matchup.matchupbackend.dto.response.mentoring.MentoringSearchResponse;
import com.example.matchup.matchupbackend.dto.response.team.*;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.TeamService;
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
@WebMvcTest(controllers = TeamController.class)
@WithMockUser
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    @MockBean
    private TokenProvider tokenProvider;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("팀 목록 조회 성공")
    void showTeams_success() throws Exception {
        Mockito.when(teamService.searchSliceTeamResponseList(any(TeamSearchRequest.class), any(Pageable.class)))
                .thenReturn(Mockito.mock(SliceTeamResponse.class));

        mockMvc.perform(get("/api/v1/list/team")
                        .param("type", "0") // 필수 파라미터 추가
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 생성 성공")
    void makeTeam_success() throws Exception {
        String requestJson = "{"
                + "\"imageBase64\": \"base64이미지문자열\","
                + "\"imageName\": \"team.png\","
                + "\"name\": \"테스트팀\","
                + "\"type\": {"
                +     "\"teamType\": 0,"
                +     "\"detailType\": \"웹프로젝트\""
                + "},"
                + "\"description\": \"팀 설명입니다.\","
                + "\"meetingSpot\": {"
                +     "\"onOffline\": \"오프라인\","
                +     "\"city\": \"서울시 강남구\","
                +     "\"detailSpot\": \"카페\""
                + "},"
                + "\"meetingDate\": \"2024-07-01\","
                + "\"memberList\": ["
                +     "{"
                +         "\"role\": \"BE\","
                +         "\"stacks\": [\"Java\", \"Spring\"],"
                +         "\"maxCount\": 2"
                +     "},"
                +     "{"
                +         "\"role\": \"FE\","
                +         "\"stacks\": [\"React\"],"
                +         "\"maxCount\": 1"
                +     "}"
                + "]"
                + "}";
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(teamService.makeNewTeam(anyLong(), any(TeamCreateRequest.class))).thenReturn(100L);

        mockMvc.perform(post("/api/v1/team")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("팀 수정 성공")
    void updateTeam_success() throws Exception {

        String requestJson = "{"
                + "\"imageBase64\": \"base64이미지문자열\","
                + "\"imageName\": \"team.png\","
                + "\"name\": \"테스트팀\","
                + "\"type\": {"
                +     "\"teamType\": 0,"
                +     "\"detailType\": \"웹프로젝트\""
                + "},"
                + "\"description\": \"팀 설명입니다.\","
                + "\"meetingSpot\": {"
                +     "\"onOffline\": \"오프라인\","
                +     "\"city\": \"서울시 강남구\","
                +     "\"detailSpot\": \"카페\""
                + "},"
                + "\"meetingDate\": \"2024-07-01\","
                + "\"memberList\": ["
                +     "{"
                +         "\"role\": \"BE\","
                +         "\"stacks\": [\"Java\", \"Spring\"],"
                +         "\"maxCount\": 2"
                +     "},"
                +     "{"
                +         "\"role\": \"FE\","
                +         "\"stacks\": [\"React\"],"
                +         "\"maxCount\": 1"
                +     "}"
                + "]"
                + "}";

        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        mockMvc.perform(put("/api/v1/team/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 삭제 성공")
    void deleteTeam_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/team/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 상세 정보 조회 성공")
    void showTeamInfo_success() throws Exception {
        Mockito.when(teamService.getTeamInfo(anyLong()))
                .thenReturn(Mockito.mock(TeamDetailResponse.class));

        mockMvc.perform(get("/api/v1/team/1/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 모임 장소 조회 성공")
    void showTeamMeetingSpot_success() throws Exception {
        Mockito.when(teamService.getTeamMeetingSpot(anyLong()))
                .thenReturn(Mockito.mock(MeetingSpotResponse.class));

        mockMvc.perform(get("/api/v1/team/1/spot"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 멘토링 목록 조회 성공")
    void showTeamMentoringList_success() throws Exception {
        Mockito.when(teamService.getTeamMentoringCardList(anyLong()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/team/1/mentoring"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 스택 목록 조회 성공")
    void showTeamTagList_success() throws Exception {
        Mockito.when(teamService.getTeamTagStringList(anyLong()))
                .thenReturn(Collections.singletonList("Java"));

        mockMvc.perform(get("/api/v1/team/1/stacks"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 타입 조회 성공")
    void showTeamType_success() throws Exception {
        Mockito.when(teamService.getTeamType(anyLong()))
                .thenReturn(Mockito.mock(TeamTypeResponse.class));

        mockMvc.perform(get("/api/v1/team/1/type"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 좋아요 상태 조회 성공")
    void checkTeamLike_success() throws Exception {
        Mockito.when(teamService.getTeamLikes(anyString(), anyLong()))
                .thenReturn(Mockito.mock(TeamLikeResponse.class));

        mockMvc.perform(get("/api/v1/team/1/like")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 좋아요 추가 성공")
    void addTeamLike_success() throws Exception {
        Mockito.when(teamService.likeTeam(anyString(), anyLong())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/team/1/like")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("팀 좋아요 취소 성공")
    void deleteTeamLike_success() throws Exception {
        Mockito.when(teamService.undoLikeTeam(anyString(), anyLong())).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/team/1/like")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("팀 상세 멘토링 목록 조회 성공")
    void getMentoringsInTeamPage_success() throws Exception {
        Mockito.when(teamService.showMentoringsInTeamPage(anyString(), anyLong()))
                .thenReturn(Collections.singletonList(Mockito.mock(MentoringSearchResponse.class)));

        mockMvc.perform(get("/api/v1/team/1/mentorings")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("팀 종료 성공")
    void finishTeam_success() throws Exception {
        mockMvc.perform(post("/api/v1/team/1/finish")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }
}