package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.request.enterprise.EnterpriseStateRequest;
import com.example.matchup.matchupbackend.dto.request.enterprise.EnterpriseVerifyFormRequest;
import com.example.matchup.matchupbackend.entity.EnterpriseVerify;
import com.example.matchup.matchupbackend.entity.Role;
import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotPermitEx.AdminOnlyPermitException;
import com.example.matchup.matchupbackend.repository.EnterpriseVerifyRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class EnterpriseServiceTest {

    private UserRepository userRepository;
    private EnterpriseVerifyRepository enterpriseVerifyRepository;
    private AlertCreateService alertCreateService;
    private EnterpriseService enterpriseService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        enterpriseVerifyRepository = mock(EnterpriseVerifyRepository.class);
        alertCreateService = mock(AlertCreateService.class);
        enterpriseService = new EnterpriseService(userRepository, enterpriseVerifyRepository, alertCreateService);
    }

    @Test
    void applyVerifyEnterprise_success() {
        Long userId = 1L;
        User user = mock(User.class);
        EnterpriseVerifyFormRequest formRequest = mock(EnterpriseVerifyFormRequest.class);
        EnterpriseVerify enterpriseVerify = mock(EnterpriseVerify.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        try (MockedStatic<EnterpriseVerify> staticMock = mockStatic(EnterpriseVerify.class)) {
            staticMock.when(() -> EnterpriseVerify.from(formRequest, user)).thenReturn(enterpriseVerify);

            User result = enterpriseService.applyVerifyEnterprise(userId, formRequest);

            verify(enterpriseVerifyRepository).save(enterpriseVerify);
            assertEquals(user, result);
        }
    }

    @Test
    void applyVerifyEnterprise_userNotFound() {
        Long userId = 1L;
        EnterpriseVerifyFormRequest formRequest = mock(EnterpriseVerifyFormRequest.class);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> enterpriseService.applyVerifyEnterprise(userId, formRequest));
    }

    @Test
    void showEnterpriseVerifyList_notAdmin() {
        Long adminId = 1L;
        User user = mock(User.class);
        when(user.getRole()).thenReturn(Role.USER);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(user));

        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(AdminOnlyPermitException.class,
                () -> enterpriseService.showEnterpriseVerifyList(adminId, pageable));
    }

    @Test
    void changeEnterpriseVerifyState_success_accepted() {
        Long adminId = 1L;
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        EnterpriseStateRequest stateRequest = mock(EnterpriseStateRequest.class);
        when(stateRequest.getEnterpriseApplyId()).thenReturn(2L);
        when(stateRequest.getIsAccepted()).thenReturn(true);

        EnterpriseVerify verify = mock(EnterpriseVerify.class);
        User user = mock(User.class);
        when(enterpriseVerifyRepository.findByIdJoinUser(2L)).thenReturn(Optional.of(verify));
        when(verify.getUser()).thenReturn(user);

        User result = enterpriseService.changeEnterpriseVerifyState(adminId, stateRequest);

        verify(verify).changeState(true);
        verify(user).changeRoleToUser();
        verify(alertCreateService).enterpriseVerifyAlert(user, false);
        assertEquals(user, result);
    }

    @Test
    void changeEnterpriseVerifyState_success_rejected() {
        Long adminId = 1L;
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        EnterpriseStateRequest stateRequest = mock(EnterpriseStateRequest.class);
        when(stateRequest.getEnterpriseApplyId()).thenReturn(2L);
        when(stateRequest.getIsAccepted()).thenReturn(false);

        EnterpriseVerify verify = mock(EnterpriseVerify.class);
        User user = mock(User.class);
        when(enterpriseVerifyRepository.findByIdJoinUser(2L)).thenReturn(Optional.of(verify));
        when(verify.getUser()).thenReturn(user);

        User result = enterpriseService.changeEnterpriseVerifyState(adminId, stateRequest);

        verify(verify).changeState(false);
        verify(user).changeRoleToEnterprise();
        verify(alertCreateService).enterpriseVerifyAlert(user, true);
        assertEquals(user, result);
    }

    @Test
    void changeEnterpriseVerifyState_notAdmin() {
        Long adminId = 1L;
        User user = mock(User.class);
        when(user.getRole()).thenReturn(Role.USER);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(user));

        EnterpriseStateRequest stateRequest = mock(EnterpriseStateRequest.class);

        assertThrows(AdminOnlyPermitException.class,
                () -> enterpriseService.changeEnterpriseVerifyState(adminId, stateRequest));
    }

    @Test
    void changeEnterpriseVerifyState_notFound() {
        Long adminId = 1L;
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        EnterpriseStateRequest stateRequest = mock(EnterpriseStateRequest.class);
        when(stateRequest.getEnterpriseApplyId()).thenReturn(2L);

        when(enterpriseVerifyRepository.findByIdJoinUser(2L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> enterpriseService.changeEnterpriseVerifyState(adminId, stateRequest));
    }
}