package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.alert.AlertFilterRequest;
import com.example.matchup.matchupbackend.dto.response.alert.SliceAlertResponse;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.AlertService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
@WithMockUser
@WebMvcTest(controllers = AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertService alertService;

    @MockBean
    private TokenProvider tokenProvider;

    private static final String AUTH_HEADER = "Bearer testtoken";
    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("알림 리스트 조회 성공")
    void getSliceAlertResponse_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(USER_ID);
        Mockito.when(alertService.getSliceAlertResponse(anyLong(), any(AlertFilterRequest.class), any(Pageable.class)))
                .thenReturn(new SliceAlertResponse(
                        List.of(), // 빈 리스트로 테스트
                        0, // 페이지 크기
                        false // 다음 페이지 없음
                ));

        mockMvc.perform(get("/api/v1/alert")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("알림 읽음 표시 성공")
    void setAlertStatusRead_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(USER_ID);

        mockMvc.perform(post("/api/v1/alert/read/10")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("alertId: 10 - 읽어졌습니다."));
    }

    @Test
    @DisplayName("알림 삭제 성공")
    void deleteAlert_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(USER_ID);

        mockMvc.perform(post("/api/v1/alert/delete/20")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(content().string("alertId: 20 - 삭제 되었습니다."));
    }
}