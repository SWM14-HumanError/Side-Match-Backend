package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.request.servicecenter.OneOnOneInquiryRequest;
import com.example.matchup.matchupbackend.dto.response.servicecenter.OneOnOneInquiryResponse;
import com.example.matchup.matchupbackend.entity.ServiceCenter;
import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.repository.ServiceCenterRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceCenterServiceTest {

    private TokenProvider tokenProvider;
    private UserRepository userRepository;
    private ServiceCenterRepository serviceCenterRepository;
    private ServiceCenterService serviceCenterService;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(TokenProvider.class);
        userRepository = mock(UserRepository.class);
        serviceCenterRepository = mock(ServiceCenterRepository.class);
        serviceCenterService = new ServiceCenterService(tokenProvider, userRepository, serviceCenterRepository);
    }

    @Test
    void postOneOnOneInquiry_성공() {
        String token = "Bearer test";
        Long userId = 1L;
        User user = mock(User.class);
        OneOnOneInquiryRequest request = mock(OneOnOneInquiryRequest.class);
        ServiceCenter inquiry = mock(ServiceCenter.class);

        when(tokenProvider.getUserId(eq(token), anyString())).thenReturn(userId);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        mockStatic(ServiceCenter.class).when(() -> ServiceCenter.createOneOnOneInquiry(request, user)).thenReturn(inquiry);

        serviceCenterService.postOneOnOneInquiry(token, request);

        verify(serviceCenterRepository).save(inquiry);
    }

    @Test
    void postOneOnOneInquiry_유저없음_예외() {
        String token = "Bearer test";
        Long userId = 1L;
        OneOnOneInquiryRequest request = mock(OneOnOneInquiryRequest.class);

        when(tokenProvider.getUserId(eq(token), anyString())).thenReturn(userId);
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> serviceCenterService.postOneOnOneInquiry(token, request));
    }

    @Test
    void getInquiryResponse_성공() {
        String token = "Bearer test";
        Long userId = 1L;
        User user = mock(User.class);
        Pageable pageable = PageRequest.of(0, 10);
        Slice<ServiceCenter> slice = mock(Slice.class);
        OneOnOneInquiryResponse response = mock(OneOnOneInquiryResponse.class);

        when(tokenProvider.getUserId(eq(token), anyString())).thenReturn(userId);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        when(serviceCenterRepository.joinUserOrderByCreatedTime(pageable)).thenReturn(slice);
        mockStatic(OneOnOneInquiryResponse.class).when(() -> OneOnOneInquiryResponse.from(slice)).thenReturn(response);

        OneOnOneInquiryResponse result = serviceCenterService.getInquiryResponse(token, pageable);

        assertEquals(response, result);
        verify(user).isAdmin();
    }

    @Test
    void getInquiryResponse_유저없음_예외() {
        String token = "Bearer test";
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(tokenProvider.getUserId(eq(token), anyString())).thenReturn(userId);
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> serviceCenterService.getInquiryResponse(token, pageable));
    }
}