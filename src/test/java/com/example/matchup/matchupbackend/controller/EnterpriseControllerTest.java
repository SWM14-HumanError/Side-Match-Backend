package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.enterprise.EnterpriseStateRequest;
import com.example.matchup.matchupbackend.dto.request.enterprise.EnterpriseVerifyFormRequest;
import com.example.matchup.matchupbackend.dto.response.enterprise.SliceEnterpriseApply;
import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.service.EnterpriseService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WithMockUser
@WebMvcTest(controllers = EnterpriseController.class)
class EnterpriseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;

    @MockBean
    private EnterpriseService enterpriseService;

    private static final String AUTH_HEADER = "Bearer testtoken";
    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("기업 인증 신청 성공")
    void postEnterpriseVerifyForm_success() throws Exception {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getEmail()).thenReturn("test@company.com");
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(USER_ID);
        Mockito.when(enterpriseService.applyVerifyEnterprise(anyLong(), any(EnterpriseVerifyFormRequest.class))).thenReturn(user);

        String requestJson = "{"
                + "\"content\":\"테스트 기업 설명\","
                + "\"enterpriseEmail\":\"test@company.com\""
                + "}";

        mockMvc.perform(post("/api/v1/enterprise/verify")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().string("기업 인증 신청 완료."));
    }

    @Test
    @DisplayName("기업 인증 신청 목록 조회 성공")
    void getEnterpriseVerifyList_success() throws Exception {
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(USER_ID);
        Mockito.when(enterpriseService.showEnterpriseVerifyList(anyLong(), any(Pageable.class)))
                .thenReturn(Mockito.mock(SliceEnterpriseApply.class));

        mockMvc.perform(get("/api/v1/enterprise/verify/list")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("기업 인증 상태 변경 성공")
    void changeEnterpriseVerifyState_success() throws Exception {
        // given
        User user = Mockito.mock(User.class);
        Mockito.when(user.getEmail()).thenReturn("test@company.com");
        Mockito.when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(USER_ID);
        Mockito.when(enterpriseService.changeEnterpriseVerifyState(anyLong(), any(EnterpriseStateRequest.class))).thenReturn(user);

        // 승인 완료 케이스 (isAccepted: false)
        mockMvc.perform(post("/api/v1/enterprise/verify/change")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enterpriseApplyId\":123,\"isAccepted\":false}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("기업 인증 승인 완료."));

        // 취소 완료 케이스 (isAccepted: true)
        mockMvc.perform(post("/api/v1/enterprise/verify/change")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enterpriseApplyId\":123,\"isAccepted\":true}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("기업 인증 취소 완료."));
    }
}