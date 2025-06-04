package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.UserCardResponse;
import com.example.matchup.matchupbackend.dto.request.user.ProfileCreateRequest;
import com.example.matchup.matchupbackend.dto.response.user.SliceUserCardResponse;
import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.error.exception.InvalidValueEx.InvalidUserDeleteException;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.global.config.jwt.TokenService;
import com.example.matchup.matchupbackend.repository.InviteTeamRepository;
import com.example.matchup.matchupbackend.repository.TeamUserRepository;
import com.example.matchup.matchupbackend.repository.UserPositionRepository;
import com.example.matchup.matchupbackend.repository.UserProfileRepository;
import com.example.matchup.matchupbackend.repository.mentoring.MentoringRepository;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserPositionRepository userPositionRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private FileService fileService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private TeamRepository teamRepository;

    @Test
    void testSearchSliceUserCard_Success() {
        // 준비: 샘플 User 리스트 및 Slice 객체
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getPictureUrl()).thenReturn("url");
        when(user.getUserLevel()).thenReturn(1L);
        when(user.getNickname()).thenReturn("testNick");
        when(user.getFeedbackScore()).thenReturn(4.5);
        when(user.getLikes()).thenReturn(10L);
        // 사용자에 대한 기술 스택은 빈 목록 반환
        when(user.returnStackList()).thenReturn(Collections.emptyList());
        List<User> userList = List.of(user);
        Slice<User> slice = new SliceImpl<>(userList, PageRequest.of(0, 10), false);
        when(userRepository.findUserListByUserRequest(any(), any())).thenReturn(slice);
        when(userPositionRepository.findAllJoinUserBy(userList)).thenReturn(Collections.emptyList());

        SliceUserCardResponse response = userService.searchSliceUserCard(null, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getUserCardResponses().size());
        UserCardResponse card = response.getUserCardResponses().get(0);
        assertEquals(1L, card.getUserID());
        assertEquals("없음", card.getPosition().getPositionName());
    }

    @Test
    void testSaveAdditionalUserInfo_Success() {
        var request = mock(com.example.matchup.matchupbackend.dto.request.user.ProfileCreateRequest.class);
        when(request.getEmail()).thenReturn("test@test.com");
        when(request.getId()).thenReturn(100L);
        when(request.getProfileTagPositions()).thenReturn(Collections.emptyList());
        when(request.getPictureUrl()).thenReturn("imageBase64");
        when(request.getImageName()).thenReturn("img.png");

        User user = mock(User.class);
        when(user.getAgreeTermOfServiceId()).thenReturn(100L);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(user.updateFirstLogin(any(), any())).thenReturn(user);
        var uploadFile = mock(com.example.matchup.matchupbackend.dto.UploadFile.class);
        when(fileService.storeBase64ToFile("imageBase64", "img.png")).thenReturn(uploadFile);
        // 모든 인자에 matcher 사용
        when(tokenProvider.generateToken(eq(user), any())).thenReturn("access-token");

        String token = userService.saveAdditionalUserInfo(request);
        assertEquals("access-token", token);
        verify(userProfileService, times(1)).updateUserTags(any(), eq(user));
        verify(userPositionRepository, times(1)).saveAll(any());
        verify(userProfileRepository, times(1)).save(any());
    }

    @Test
    void testTokenRefresh_Success() {
        // 준비: HttpServletRequest, HttpServletResponse 설정 및 쿠키 세팅
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie refreshCookie = new Cookie("refresh_token", "refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        when(tokenService.createNewAccessToken("refresh-token")).thenReturn("new-access-token");
        // 실제 쿠키 삭제 및 추가는 CookieUtil 내부에서 처리되므로 호출 여부만 확인
        userService.tokenRefresh(request, response);
        verify(tokenService, times(1)).createNewAccessToken("refresh-token");
    }

    @Test
    void testUpdateUserLastLogin_Success() {
        // 준비: tokenProvider와 userRepository stub
        when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        User user = mock(User.class);
        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));

        userService.updateUserLastLogin("auth-header");
        // verify user의 updateUserLastLogin 호출
        verify(user, times(1)).updateUserLastLogin();
    }

    @Test
    void testDeleteUser_Success() {
        // 준비: tokenProvider와 userRepository stub, 삭제 가능한 사용자
        when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(1L);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getIsDeleted()).thenReturn(false);
        when(user.getIsMentor()).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // 팀 탈퇴 조건: 팀 Repository에서 비어있는 리스트 반환
        when(teamRepository.findActiveTeamByLeaderID(1L)).thenReturn(Collections.emptyList());
        // deleteUser 메서드 내에서 isDeletableUser 가 통과되어 deleteUser() 호출

        userService.deleteUser("token");
        verify(user, times(1)).deleteUser();
    }

    @Test
    void testDeleteUser_AlreadyDeleted() {
        // 준비: 이미 탈퇴한 유저의 경우
        when(tokenProvider.getUserId(anyString(), anyString())).thenReturn(2L);
        User user = mock(User.class);
        when(user.getId()).thenReturn(2L);
        when(user.getIsDeleted()).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(InvalidUserDeleteException.class, () -> userService.deleteUser("token"));
    }
}