package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.user.ProfileCreateRequest;
import com.example.matchup.matchupbackend.dto.request.user.UserSearchRequest;
import com.example.matchup.matchupbackend.dto.response.user.InviteMyTeamResponse;
import com.example.matchup.matchupbackend.dto.response.user.SliceUserCardResponse;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.AlertCreateService;
import com.example.matchup.matchupbackend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
@WithMockUser
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;
    @MockBean
    AlertCreateService alertCreateService;
    @MockBean
    TokenProvider tokenProvider;

    @Test
    @DisplayName("GET /api/v1/list/user - 유저 리스트 조회")
    void showUsers() throws Exception {
        Mockito.when(userService.searchSliceUserCard(any(UserSearchRequest.class), any(Pageable.class)))
                .thenReturn(Mockito.mock(SliceUserCardResponse.class));

        mockMvc.perform(get("/api/v1/list/user"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/login/user/info - 추가 유저 정보 저장")
    void additionalUserInfo() throws Exception {
        String requestJson = "{"
                + "\"nickname\":\"테스트닉네임\","
                + "\"profileImageUrl\":\"http://test.com/img.png\","
                + "\"birth\":\"2000-01-01\","
                + "\"careerYear\":2"
                + "}";

        Mockito.when(userService.saveAdditionalUserInfo(any(ProfileCreateRequest.class)))
                .thenReturn("ok");

        mockMvc.perform(put("/api/v1/login/user/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/v1/login/token/refresh - 토큰 리프레시")
    void loginToken() throws Exception {
        mockMvc.perform(get("/api/v1/login/token/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/user/online - 유저 온라인 상태 갱신")
    void isUserOnline() throws Exception {
        mockMvc.perform(get("/api/v1/user/online")
                        .header("Authorization", "Bearer testtoken"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/user/invite - 내 프로젝트 초대 목록 조회")
    void showInviteMyTeam() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(userService.getInviteMyTeam(anyLong(), anyLong()))
                .thenReturn(Mockito.mock(InviteMyTeamResponse.class));

        mockMvc.perform(get("/api/v1/user/invite")
                        .header("Authorization", "Bearer testtoken")
                        .param("receiver", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/user/invite - 내 프로젝트 초대 요청")
    void suggestInviteMyTeam() throws Exception {
        String requestJson = "{"
                + "\"teamId\":1,"
                + "\"receiverId\":2,"
                + "\"content\":\"초대글 내용입니다.\""
                + "}";
        mockMvc.perform(post("/api/v1/user/invite")
                        .header("Authorization", "Bearer testtoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /api/v1/user/delete - 회원 탈퇴")
    void deleteUser() throws Exception {
        mockMvc.perform(delete("/api/v1/user/delete")
                        .header("Authorization", "Bearer testtoken"))
                .andExpect(status().isOk());
    }
}