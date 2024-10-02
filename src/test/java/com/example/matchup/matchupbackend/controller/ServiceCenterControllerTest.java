package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.servicecenter.OneOnOneInquiryRequest;
import com.example.matchup.matchupbackend.dto.response.servicecenter.OneOnOneInquiryResponse;
import com.example.matchup.matchupbackend.service.ServiceCenterService;
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
@WebMvcTest(controllers = ServiceCenterController.class)
@WithMockUser
class ServiceCenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceCenterService serviceCenterService;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("1:1 문의 등록 성공")
    void receiveOneOnOneInquiry_success() throws Exception {
        String requestJson = "{ \"title\": \"문의 제목\", \"content\": \"문의 내용\" }"; // 실제 필드에 맞게 수정

        mockMvc.perform(post("/api/v1/inquiry")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("1:1 문의 내역 조회 성공")
    void showOneOnOneInquiry_success() throws Exception {
        Mockito.when(serviceCenterService.getInquiryResponse(anyString(), any(Pageable.class)))
                .thenReturn(Mockito.mock(OneOnOneInquiryResponse.class));

        mockMvc.perform(get("/api/v1/inquiry")
                        .header("Authorization", AUTH_HEADER)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }
}