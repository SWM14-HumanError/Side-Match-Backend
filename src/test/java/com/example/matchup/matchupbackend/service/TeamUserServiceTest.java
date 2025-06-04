package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.request.teamuser.*;
import com.example.matchup.matchupbackend.dto.response.teamuser.RecruitInfoResponse;
import com.example.matchup.matchupbackend.dto.response.teamuser.RefuseReasonResponse;
import com.example.matchup.matchupbackend.dto.response.teamuser.TeamApprovedInfoResponse;
import com.example.matchup.matchupbackend.dto.response.teamuser.TeamUserCardResponse;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.error.exception.InvalidValueEx.InvalidFeedbackException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.TeamPositionNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.TeamUserNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotPermitEx.LeaderOnlyPermitException;
import com.example.matchup.matchupbackend.global.RoleType;
import com.example.matchup.matchupbackend.repository.*;
import com.example.matchup.matchupbackend.repository.feedback.FeedbackRepository;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamUserServiceTest {

    @InjectMocks
    private TeamUserService teamUserService;

    @Mock
    private TeamUserRepository teamUserRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamPositionRepository teamPositionRepository;
    @Mock
    private TeamRecruitRepository teamRecruitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private UserPositionRepository userPositionRepository;
    @Mock
    private TeamRefuseRepository teamRefuseRepository;
    @Mock
    private AlertCreateService alertCreateService;

    // getTeamUserCard 테스트 (팀장, 팀원, 일반 사용자, 예외)
    @Test
    void testGetTeamUserCard_AsLeader() {
        Long teamId = 1L;
        Long leaderId = 100L;
        // 팀 조회: 팀 리더임
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(leaderId);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));

        // 팀장용 메서드: findTeamUserJoinUserAndRecruit 호출
        TeamUser leaderTeamUser = mock(TeamUser.class);
        User leader = mock(User.class);
        when(leaderTeamUser.getUser()).thenReturn(leader);
        List<TeamUser> teamUsers = Collections.singletonList(leaderTeamUser);
        when(teamUserRepository.findTeamUserJoinUserAndRecruit(teamId)).thenReturn(teamUsers);
        when(feedbackRepository.findFeedbacksJoinReceiverBy(leaderId, teamId))
                .thenReturn(Collections.emptyList());
        when(userPositionRepository.findAllByUser(any())).thenReturn(Collections.emptyList());

        List<TeamUserCardResponse> responses = teamUserService.getTeamUserCard(leaderId, teamId);
        assertNotNull(responses);
        assertThat(responses).hasSize(1);
    }

    @Test
    void testGetTeamUserCard_AsTeamMember() {
        Long teamId = 2L;
        Long userId = 200L;
        // 팀 멤버인 경우 존재 여부 true
        when(teamUserRepository.existsByTeamIdAndUserIdAndApproveTrue(teamId, userId)).thenReturn(true);

        // 팀 조회 시 팀 객체 생성 및 팀 리더가 team 멤버가 아니도록 세팅
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(999L); // 팀 리더와 다른 값
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));

        // 팀원용 메서드: findAcceptedTeamUserByTeamID 호출
        TeamUser member = mock(TeamUser.class);
        User memberUser = mock(User.class);
        when(member.getUser()).thenReturn(memberUser);
        List<TeamUser> acceptedMembers = Collections.singletonList(member);
        when(teamUserRepository.findAcceptedTeamUserByTeamID(teamId)).thenReturn(acceptedMembers);
        // 피드백이 없을 경우
        when(feedbackRepository.findFeedbacksJoinReceiverBy(userId, teamId))
                .thenReturn(Collections.emptyList());
        when(userPositionRepository.findAllByUser(memberUser)).thenReturn(Collections.emptyList());

        List<TeamUserCardResponse> responses = teamUserService.getTeamUserCard(userId, teamId);
        assertNotNull(responses);
        assertThat(responses).hasSize(1);
    }

    @Test
    void testGetTeamUserCard_AsGeneralUser() {
        Long teamId = 3L;
        Long userId = 300L;
        // 일반 사용자인 경우 (팀장아님, 가입된 팀원이 아님)
        when(teamUserRepository.existsByTeamIdAndUserIdAndApproveTrue(teamId, userId))
                .thenReturn(false);

        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(999L); // 팀 리더와 다른 값
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));

        TeamUser member = mock(TeamUser.class);
        User memberUser = mock(User.class);
        when(member.getUser()).thenReturn(memberUser);
        List<TeamUser> acceptedMembers = Collections.singletonList(member);
        when(teamUserRepository.findAcceptedTeamUserByTeamID(teamId)).thenReturn(acceptedMembers);

        // 일반 사용자의 경우 피드백 및 userPosition 조회 스텁
        when(feedbackRepository.findFeedbacksJoinReceiverBy(userId, teamId))
                .thenReturn(Collections.emptyList());
        when(userPositionRepository.findAllByUser(memberUser)).thenReturn(Collections.emptyList());

        List<TeamUserCardResponse> responses = teamUserService.getTeamUserCard(userId, teamId);
        assertNotNull(responses);
        assertThat(responses).hasSize(1);
    }

    @Test
    void testGetTeamUserCard_NoTeamUsers_Exception() {
        Long teamId = 4L;
        Long userId = 400L;
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(300L);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        when(teamUserRepository.existsByTeamIdAndUserIdAndApproveTrue(teamId, userId)).thenReturn(true);
        when(teamUserRepository.findAcceptedTeamUserByTeamID(teamId)).thenReturn(Collections.emptyList());
        when(feedbackRepository.findFeedbacksJoinReceiverBy(userId, teamId))
                .thenReturn(Collections.emptyList());
        assertThrows(TeamUserNotFoundException.class, () -> teamUserService.getTeamUserCard(userId, teamId));
    }

    @Test
    void testGetTeamApprovedMemberInfo_Success() {
        Long teamId = 5L;
        // 팀 객체 목킹
        Team team = mock(Team.class);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));

        // 팀 포지션 목킹: getRole()에 RoleType 반환
        TeamPosition pos = mock(TeamPosition.class);
        when(pos.getMaxCount()).thenReturn(3L);
        when(pos.getRole()).thenReturn(RoleType.FE);
        // pos.getTeam()이 올바른 팀 객체를 반환하도록 스텁 처리
        when(pos.getTeam()).thenReturn(team);
        // 팀의 numberOfUserByPosition 메서드 호출 시 예시로 1L 반환 설정
        when(team.numberOfUserByPosition(any())).thenReturn(1L);

        List<TeamPosition> positions = Collections.singletonList(pos);
        when(teamPositionRepository.findTeamPositionListByTeamId(teamId)).thenReturn(positions);

        TeamApprovedInfoResponse response = teamUserService.getTeamApprovedMemberInfo(teamId);
        assertNotNull(response);
    }

    @Test
    void testGetTeamApprovedMemberInfo_NoTeamPositions_Exception() {
        Long teamId = 6L;
        when(teamPositionRepository.findTeamPositionListByTeamId(teamId)).thenReturn(Collections.emptyList());
        assertThrows(TeamPositionNotFoundException.class, () -> teamUserService.getTeamApprovedMemberInfo(teamId));
    }

    @Test
    void testNumberOfMaxTeamMember() {
        TeamPosition pos1 = mock(TeamPosition.class);
        TeamPosition pos2 = mock(TeamPosition.class);
        when(pos1.getMaxCount()).thenReturn(2L);
        when(pos2.getMaxCount()).thenReturn(3L);
        List<TeamPosition> positions = Arrays.asList(pos1, pos2);
        Long total = teamUserService.numberOfMaxTeamMember(positions);
        assertEquals(5L, total);
    }

    @Test
    void testRecruitToTeam_Success() {
        Long teamId = 7L;
        Long userId = 700L;
        Long leaderId = 1000L;
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(leaderId);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));

        User user = mock(User.class);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));
        TeamPosition teamPosition = mock(TeamPosition.class);
        when(teamPositionRepository.findTeamPositionByTeamIdAndRole(eq(teamId), any())).thenReturn(Optional.of(teamPosition));
        when(teamUserRepository.isUserRecruitDuplicated(userId, teamId)).thenReturn(Collections.emptyList());

        TeamRecruit teamRecruit = mock(TeamRecruit.class);
        when(teamRecruitRepository.save(any(TeamRecruit.class))).thenReturn(teamRecruit);
        User leader = mock(User.class);
        when(userRepository.findById(leaderId)).thenReturn(Optional.of(leader));

        TeamUser teamUser = mock(TeamUser.class);
        when(teamUser.getId()).thenReturn(999L);
        when(teamUserRepository.save(any(TeamUser.class))).thenReturn(teamUser);

        RecruitFormRequest recruitForm = mock(RecruitFormRequest.class);
        Long resultId = teamUserService.recruitToTeam(userId, teamId, recruitForm);
        assertEquals(999L, resultId);
    }

    @Test
    void testIsRecruitAvailable_True() {
        Long teamId = 9L;
        Long userId = 900L;
        when(teamUserRepository.isUserRecruitDuplicated(userId, teamId)).thenReturn(Collections.emptyList());
        assertTrue(teamUserService.isRecruitAvailable(userId, teamId));
    }

    /*


    @Test
    void testIsRecruitAvailable_False() {
        Long teamId = 10L;
        Long userId = 1000L;
        when(teamUserRepository.isUserRecruitDuplicated(userId, teamId)).thenReturn(Arrays.asList(new TeamUser()));
        assertFalse(teamUserService.isRecruitAvailable(userId, teamId));
    }*/

    @Test
    void testAcceptUserToTeam_Success() {
        Long teamId = 11L;
        Long leaderId = 1100L;
        AcceptFormRequest acceptForm = mock(AcceptFormRequest.class);
        when(acceptForm.getRecruitUserID()).thenReturn(555L);

        // 팀장 검증
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(leaderId);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        // 지원자 조회 및 recruitUser의 Team, Role 스텁 추가
        TeamUser recruitUser = mock(TeamUser.class);
        Team teamForRecruitUser = mock(Team.class);
        when(recruitUser.getTeam()).thenReturn(teamForRecruitUser);
        when(teamForRecruitUser.getId()).thenReturn(teamId);
        when(recruitUser.getRole()).thenReturn(RoleType.BE);
        when(teamUserRepository.findTeamUserJoinTeamAndUser(teamId, 555L)).thenReturn(Optional.of(recruitUser));

        // teamPosition 조회
        TeamPosition teamPosition = mock(TeamPosition.class);
        when(teamPositionRepository.findTeamPositionByTeamIdAndRole(anyLong(), any()))
                .thenReturn(Optional.of(teamPosition));

        // validAcceptUserToTeam 내 비교를 위해 count, maxCount 스텁 추가
        when(teamPosition.getCount()).thenReturn(0L);
        when(teamPosition.getMaxCount()).thenReturn(3L);

        teamUserService.acceptUserToTeam(leaderId, teamId, acceptForm);
        verify(alertCreateService, times(1)).saveUserAcceptedToTeamAlert(any(), eq(recruitUser), eq(acceptForm));
    }

    @Test
    void testAcceptUserToTeam_NotLeader_Exception() {
        Long teamId = 12L;
        Long nonLeaderId = 1200L;
        AcceptFormRequest acceptForm = mock(AcceptFormRequest.class);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(mock(Team.class)));
        // isTeamLeader 결과 false 처리
        assertThrows(LeaderOnlyPermitException.class, () -> teamUserService.acceptUserToTeam(nonLeaderId, teamId, acceptForm));
    }

    @Test
    void testRefuseUserToTeam_Success() {
        Long teamId = 13L;
        Long leaderId = 1300L;
        Long recruitUserId = 777L;
        RefuseFormRequest refuseForm = mock(RefuseFormRequest.class);
        when(refuseForm.getRecruitUserID()).thenReturn(recruitUserId);
        when(refuseForm.getRefuseReason()).thenReturn("거절 사유");
        // 팀 조회 및 리더 설정
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(leaderId);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        // 지원 유저에 대한 TeamUser 목킹
        TeamUser teamUser = mock(TeamUser.class);
        when(teamUserRepository.findTeamUserJoinTeamAndUser(teamId, recruitUserId))
                .thenReturn(Optional.of(teamUser));
        // TeamRefuse 저장 시 목킹
        TeamRefuse teamRefuse = mock(TeamRefuse.class);
        when(teamRefuse.getId()).thenReturn(555L);
        when(teamRefuseRepository.save(any())).thenReturn(teamRefuse);
        // 리더 TeamUser 조회 설정 (alertToLeaderAndRecruiter)
        TeamUser leaderTeamUser = mock(TeamUser.class);
        when(teamUserRepository.findTeamUserJoinTeamAndUser(teamId, leaderId))
                .thenReturn(Optional.of(leaderTeamUser));

        // 지원 유저 User 조회 설정
        User recruitUser = mock(User.class);
        when(userRepository.findById(recruitUserId)).thenReturn(Optional.of(recruitUser));

        // 메서드 호출
        teamUserService.refuseUserToTeam(leaderId, teamId, refuseForm);

        // delete() 호출 및 알림 검증
        verify(teamUserRepository, times(1)).delete(teamUser);
        verify(alertCreateService, times(1)).saveUserRefusedToTeamAlert(any(), any(), eq(555L));
    }

    @Test
    void testRefuseUserToTeam_NotLeader_Exception() {
        Long teamId = 14L;
        Long nonLeaderId = 1400L;
        RefuseFormRequest refuseForm = mock(RefuseFormRequest.class);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(mock(Team.class)));
        assertThrows(LeaderOnlyPermitException.class, () -> teamUserService.refuseUserToTeam(nonLeaderId, teamId, refuseForm));
    }

    @Test
    void testKickUserToTeam_Success() {
        Long teamId = 15L;
        Long leaderId = 1500L;
        KickFormRequest kickForm = mock(KickFormRequest.class);
        when(kickForm.getKickUserID()).thenReturn(888L);
        when(kickForm.getRole()).thenReturn(null);

        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(leaderId);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));

        TeamUser kickedUser = mock(TeamUser.class);
        when(teamUserRepository.findTeamUserJoinTeamAndUser(teamId, 888L))
                .thenReturn(Optional.of(kickedUser));
        when(teamRefuseRepository.save(any())).thenReturn(mock(TeamRefuse.class));

        teamUserService.kickUserToTeam(leaderId, teamId, kickForm);

        verify(alertCreateService, times(1))
                .saveUserKickedToTeamAlert(eq(kickedUser), anyLong());
        verify(teamUserRepository, times(1))
                .deleteTeamUserByTeamIdAndUserId(eq(teamId), eq(888L));
    }

    @Test
    void testKickUserToTeam_NotLeader_Exception() {
        Long teamId = 16L;
        Long nonLeaderId = 1600L;
        KickFormRequest kickForm = mock(KickFormRequest.class);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(mock(Team.class)));
        assertThrows(LeaderOnlyPermitException.class, () -> teamUserService.kickUserToTeam(nonLeaderId, teamId, kickForm));
    }

    @Test
    void testIsTeamLeader_True() {
        Long teamId = 17L;
        Long leaderId = 1700L;
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(leaderId);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        assertTrue(teamUserService.isTeamLeader(leaderId, teamId));
    }

    @Test
    void testIsTeamLeader_False() {
        Long teamId = 18L;
        Long leaderId = 1800L;
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(1900L);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        assertFalse(teamUserService.isTeamLeader(leaderId, teamId));
    }

    @Test
    void testFeedbackToTeamUser_Success() {
        Long teamId = 19L;
        Long giverId = 1900L;
        Long receiverId = 1910L;
        TeamUserFeedbackRequest feedbackRequest = mock(TeamUserFeedbackRequest.class);
        when(feedbackRequest.getReceiverID()).thenReturn(receiverId);
        when(feedbackRequest.getGrade()).thenReturn(FeedbackGrade.GREAT);
        when(feedbackRequest.getCommentToUser()).thenReturn("Good feedback");
        when(feedbackRequest.getCommentToAdmin()).thenReturn("Nice work");
        when(feedbackRequest.getIsContactable()).thenReturn(true);
        when(feedbackRequest.getIsOnTime()).thenReturn(true);
        when(feedbackRequest.getIsResponsible()).thenReturn(true);
        when(feedbackRequest.getIsKind()).thenReturn(true);
        when(feedbackRequest.getIsCollaboration()).thenReturn(true);
        when(feedbackRequest.getIsFast()).thenReturn(true);
        when(feedbackRequest.getIsActively()).thenReturn(true);
        // giver 조회
        User giver = mock(User.class);
        when(userRepository.findById(giverId)).thenReturn(Optional.of(giver));
        // receiverTeamUser와 receiver 목킹
        TeamUser receiverTeamUser = mock(TeamUser.class);
        User receiver = mock(User.class);
        when(receiverTeamUser.getUser()).thenReturn(receiver);
        when(teamUserRepository.findTeamUserJoinTeamAndUser(teamId, receiverId))
                .thenReturn(Optional.of(receiverTeamUser));

        // 팀 삭제 여부 확인 (삭제되지 않았음)
        Team team = mock(Team.class);
        when(team.getIsDeleted()).thenReturn(0L);
        when(receiverTeamUser.getTeam()).thenReturn(team);
        // 내부에서 호출하는 팀 유저 조회 목킹
        TeamUser giverTeamUser = mock(TeamUser.class);
        when(teamUserRepository.findTeamUserByTeamIdAndUserId(teamId, giverId))
                .thenReturn(Optional.of(giverTeamUser));
        when(teamUserRepository.findTeamUserByTeamIdAndUserId(teamId, receiverId))
                .thenReturn(Optional.of(receiverTeamUser));
        // 서비스 메서드 호출
        teamUserService.feedbackToTeamUser(giverId, teamId, feedbackRequest);
        verify(alertCreateService, times(1)).saveFeedbackAlert(eq(giver), any(), any());
    }

    @Test
    void testFeedbackToTeamUser_DeletedTeam_Exception() {
        Long teamId = 20L;
        Long giverId = 2000L;
        Long receiverId = 2010L;
        TeamUserFeedbackRequest feedbackRequest = mock(TeamUserFeedbackRequest.class);
        when(feedbackRequest.getReceiverID()).thenReturn(receiverId);
        when(userRepository.findById(giverId)).thenReturn(Optional.of(mock(User.class)));
        TeamUser receiverTeamUser = mock(TeamUser.class);
        Team team = mock(Team.class);
        when(team.getIsDeleted()).thenReturn(1L);
        when(receiverTeamUser.getTeam()).thenReturn(team);
        when(teamUserRepository.findTeamUserJoinTeamAndUser(teamId, receiverId)).thenReturn(Optional.of(receiverTeamUser));
        assertThrows(InvalidFeedbackException.class, () -> teamUserService.feedbackToTeamUser(giverId, teamId, feedbackRequest));
    }

    // isPossibleFeedback 테스트
    @Test
    void testIsPossibleFeedback_Success_NoPreviousFeedback() {
        Long giverId = 2100L;
        Long receiverId = 2110L;
        Long teamId = 21L;
        when(feedbackRepository.findFeedbackByUserAndTeam(giverId, receiverId, teamId))
                .thenReturn(Collections.emptyList());
        // 메서드 실행 시 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> teamUserService.isPossibleFeedback(giverId, receiverId, teamId));
    }

    @Test
    void testIsPossibleFeedback_TooSoon_Exception() {
        Long giverId = 2200L;
        Long receiverId = 2210L;
        Long teamId = 22L;
        Feedback previousFeedback = mock(Feedback.class);
        LocalDateTime past = LocalDateTime.now().minusDays(3);
        when(previousFeedback.getCreateTime()).thenReturn(past);
        when(feedbackRepository.findFeedbackByUserAndTeam(giverId, receiverId, teamId))
                .thenReturn(Arrays.asList(previousFeedback));
        assertThrows(InvalidFeedbackException.class, () -> teamUserService.isPossibleFeedback(giverId, receiverId, teamId));
    }

    @Test
    void testIsPossibleFeedback_SameUser_Exception() {
        Long userId = 2300L;
        Long teamId = 23L;
        // giver와 receiver가 같은 경우
        when(feedbackRepository.findFeedbackByUserAndTeam(userId, userId, teamId))
                .thenReturn(Collections.emptyList());
        assertThrows(InvalidFeedbackException.class, () -> teamUserService.isPossibleFeedback(userId, userId, teamId));
    }

    @Test
    void testGetRecruitInfo_Success() {
        Long teamId = 24L;
        Long leaderId = 2400L;
        Long recruitId = 2500L;
        RecruitFormRequest dummy = mock(RecruitFormRequest.class);

        // 팀장 검증
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(leaderId);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));

        // 팀 리크루트와 관련된 목킹
        TeamRecruit teamRecruit = mock(TeamRecruit.class);
        User recruitUser = mock(User.class);
        when(recruitUser.getId()).thenReturn(recruitId);
        when(teamRecruit.getUser()).thenReturn(recruitUser);
        when(teamRecruitRepository.findRecruitJoinUserById(recruitId)).thenReturn(Optional.of(teamRecruit));

        when(userPositionRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());

        RecruitInfoResponse response = teamUserService.getRecruitInfo(leaderId, teamId, recruitId);
        assertNotNull(response);
    }

    @Test
    void testGetRecruitInfo_NotLeader_Exception() {
        Long teamId = 26L;
        Long nonLeaderId = 2600L;
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(mock(Team.class)));
        assertThrows(LeaderOnlyPermitException.class, () -> teamUserService.getRecruitInfo(nonLeaderId, teamId, 2700L));
    }

    @Test
    void testGetUserRefuseReason_Success() {
        Long userId = 2800L;
        Long refuseId = 2900L;

        TeamRefuse teamRefuse = mock(TeamRefuse.class);
        Team team = mock(Team.class);
        when(team.getLeaderID()).thenReturn(3000L);
        when(teamRefuse.getTeam()).thenReturn(team);

        User refusedUser = mock(User.class);
        when(refusedUser.getNickname()).thenReturn("refusedNickname");
        when(teamRefuse.getRefusedUser()).thenReturn(refusedUser);

        when(teamRefuseRepository.findRefuseInfoJoinUserAndTeamById(refuseId)).thenReturn(Optional.of(teamRefuse));

        User leader = mock(User.class);
        when(userRepository.findById(3000L)).thenReturn(Optional.of(leader));

        RefuseReasonResponse response = teamUserService.getUserRefuseReason(userId, refuseId);
        assertNotNull(response);
    }

    @Test
    void testGetUserRefuseReason_NotFound_Exception() {
        Long userId = 3100L;
        Long refuseId = 3200L;
        when(teamRefuseRepository.findRefuseInfoJoinUserAndTeamById(refuseId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> teamUserService.getUserRefuseReason(userId, refuseId));
    }
}