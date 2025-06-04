package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.response.team.SliceTeamResponse;
import com.example.matchup.matchupbackend.dto.response.user.SliceUserCardResponse;
import com.example.matchup.matchupbackend.dto.response.user.UserLikeResponse;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.LikeService;
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
@WebMvcTest(controllers = LikeController.class)
@WithMockUser
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private LikeService likeService;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("유저에게 좋아요 누르기 성공")
    void saveLikeToUser_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/likes/user/2")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated())
                .andExpect(content().string("userID: 2 에게 좋아요를 눌렀습니다."));
    }

    @Test
    @DisplayName("유저에게 좋아요 삭제 성공")
    void deleteLikeToUser_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/likes/user/2")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent())
                .andExpect(content().string("userID: 2 에게 준 좋아요를 삭제하였습니다."));
    }

    @Test
    @DisplayName("내가 유저에게 좋아요 눌렀는지 체크 성공")
    void checkUserLike_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(likeService.checkUserLiked(anyLong(), anyLong()))
                .thenReturn(Mockito.mock(UserLikeResponse.class));

        mockMvc.perform(get("/api/v1/likes/check/2")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내가 좋아요 누른 기업 프로젝트 목록 조회 성공")
    void getLikedProjectTeam_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(likeService.getLikedSliceProjectTeamResponse(anyLong(), any(Pageable.class)))
                .thenReturn(Mockito.mock(SliceTeamResponse.class));

        mockMvc.perform(get("/api/v1/likes/mylike/project")
                        .header("Authorization", AUTH_HEADER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내가 좋아요 누른 개인 프로젝트 목록 조회 성공")
    void getLikedStudyTeam_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(likeService.getLikedSliceStudyTeamResponse(anyLong(), any(Pageable.class)))
                .thenReturn(Mockito.mock(SliceTeamResponse.class));

        mockMvc.perform(get("/api/v1/likes/mylike/study")
                        .header("Authorization", AUTH_HEADER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내가 좋아요 누른 유저 목록 조회 성공")
    void getLikedUser_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        Mockito.when(likeService.getLikedSliceUserCardResponse(anyLong(), any(Pageable.class)))
                .thenReturn(Mockito.mock(SliceUserCardResponse.class));

        mockMvc.perform(get("/api/v1/likes/mylike/user")
                        .header("Authorization", AUTH_HEADER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }
}