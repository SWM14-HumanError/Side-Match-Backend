package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.response.profile.UserProfileDetailResponse;
import com.example.matchup.matchupbackend.dto.response.profile.UserSettingStateResponse;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.global.RoleType;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.repository.UserTagRepository;
import com.example.matchup.matchupbackend.repository.feedback.FeedbackRepository;
import com.example.matchup.matchupbackend.repository.TagRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import com.example.matchup.matchupbackend.repository.user.UserSnsLinkRepository;
import com.example.matchup.matchupbackend.repository.UserPositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @InjectMocks
    private UserProfileService userProfileService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private FileService fileService;
    @Mock
    private UserTagRepository userTagRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private UserPositionRepository userPositionRepository;
    @Mock
    private UserSnsLinkRepository userSnsLinkRepository;
    @Mock
    private FeedbackRepository feedbackRepository;

    @Test
    void testGetUserProfile_Success() {
        Long userId = 1L;
        // 모의 User, UserProfile 세팅
        User user = mock(User.class);
        UserProfile userProfile = mock(UserProfile.class);
        when(user.getUserProfile()).thenReturn(userProfile);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));

        // 최소 필요한 값 설정
        when(user.getPictureUrl()).thenReturn("sampleUrl");
        when(user.getNickname()).thenReturn("testNick");
        when(user.getUserLevel()).thenReturn(1L);
        when(user.getFeedbackScore()).thenReturn(4.5);
        when(user.getIsMentor()).thenReturn(true);
        when(user.getIsAuth()).thenReturn(true);
        when(user.getLastLogin()).thenReturn(LocalDateTime.now());
        when(userProfile.getSnsLinks()).thenReturn(Collections.emptyMap());
        when(userProfile.getIntroduce()).thenReturn("introduce");
        when(userProfile.getMeetingAddress()).thenReturn("address");
        when(userProfile.getMeetingTime()).thenReturn("time");
        when(userProfile.getMeetingType()).thenReturn(MeetingType.ONLINE);
        when(userProfile.getMeetingNote()).thenReturn("note");

        // teamUserList 설정: 삭제되지 않은 팀만 포함
        TeamUser teamUser = mock(TeamUser.class);
        Team team = mock(Team.class);
        when(team.getIsDeleted()).thenReturn(0L);
        when(team.getType()).thenReturn(0L);
        when(team.getLeaderID()).thenReturn(1L);
        when(teamUser.getApprove()).thenReturn(true);
        when(teamUser.getTeam()).thenReturn(team);
        List<TeamUser> teamUserList = new ArrayList<>();
        teamUserList.add(teamUser);
        when(user.getTeamUserList()).thenReturn(teamUserList);

        // teamService.getUserMap()에 리더 User 포함
        User leaderUser = mock(User.class);
        when(leaderUser.getNickname()).thenReturn("leaderNick");
        when(leaderUser.getUserLevel()).thenReturn(1L);
        Map<Long, User> userMap = Collections.singletonMap(1L, leaderUser);
        when(teamService.getUserMap()).thenReturn(userMap);

        // userPositions 및 userTags 빈 리스트로 처리
        List<UserPosition> userPositions = new ArrayList<>();
        when(user.getUserPositions()).thenReturn(userPositions);
        List<UserTag> userTags = new ArrayList<>();
        when(userTagRepository.findAllByUser(user)).thenReturn(userTags);

        UserProfileDetailResponse response = userProfileService.getUserProfile(userId);

        assertNotNull(response);
        assertEquals("sampleUrl", response.getPictureUrl());
        assertEquals("testNick", response.getNickname());
    }

    @Test
    void testGetUserProfile_UserNotFound() {
        Long userId = 2L;
        when(userRepository.findUserById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userProfileService.getUserProfile(userId));
    }

    @Test
    void testGetUserSettingState_Success() {
        Long userId = 3L;
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getProfileHider()).thenReturn(true);
        when(user.getFeedbackHider()).thenReturn(false);

        UserSettingStateResponse response = userProfileService.getUserSettingState(userId);
        assertNotNull(response);
        assertTrue(response.getIsProfileHider());
        assertFalse(response.getIsFeedbackHider());
    }
}