package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.request.team.TeamCreateRequest;
import com.example.matchup.matchupbackend.dto.request.teamuser.AcceptFormRequest;
import com.example.matchup.matchupbackend.dto.response.team.TeamTypeResponse;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.global.RoleType;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.repository.InviteTeamRepository;
import com.example.matchup.matchupbackend.repository.alert.AlertRepository;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AlertCreateServiceTest {

    private AlertCreateService alertCreateService;
    private AlertRepository alertRepository;
    private TokenProvider tokenProvider;
    private UserRepository userRepository;
    private TeamRepository teamRepository;
    private InviteTeamRepository inviteTeamRepository;

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        tokenProvider = mock(TokenProvider.class);
        userRepository = mock(UserRepository.class);
        teamRepository = mock(TeamRepository.class);
        inviteTeamRepository = mock(InviteTeamRepository.class);

        alertCreateService = new AlertCreateService(
                alertRepository,
                tokenProvider,
                userRepository,
                teamRepository,
                inviteTeamRepository
        );
    }

    @Test
    void testSaveTeamCreateAlert() {
        Long teamID = 1L;
        User sendTo = mock(User.class);
        TeamCreateRequest teamCreateRequest = mock(TeamCreateRequest.class);
        TeamTypeResponse teamTypeResponse = mock(TeamTypeResponse.class); // Mock 객체 추가

        when(teamCreateRequest.getType()).thenReturn(teamTypeResponse); // getType() 반환값 설정
        when(teamTypeResponse.getTeamType()).thenReturn(0L); // getTeamType() 반환값 설정
        when(teamCreateRequest.getName()).thenReturn("Test Project");

        alertCreateService.saveTeamCreateAlert(teamID, sendTo, teamCreateRequest);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testSaveTeamUpdateAlert() {
        Long teamID = 1L;
        List<User> sendTo = List.of(mock(User.class), mock(User.class));
        TeamCreateRequest teamCreateRequest = mock(TeamCreateRequest.class);
        TeamTypeResponse teamTypeResponse = mock(TeamTypeResponse.class); // Mock 객체 추가

        when(teamCreateRequest.getType()).thenReturn(teamTypeResponse); // getType() 반환값 설정
        when(teamTypeResponse.getTeamType()).thenReturn(1L); // getTeamType() 반환값 설정
        when(teamCreateRequest.getName()).thenReturn("Updated Project");

        alertCreateService.saveTeamUpdateAlert(teamID, sendTo, teamCreateRequest);

        verify(alertRepository, times(sendTo.size())).save(any(Alert.class));
    }

    @Test
    void testSaveTeamDeleteAlert() {
        List<User> sendTo = List.of(mock(User.class), mock(User.class));
        Team team = mock(Team.class);

        when(team.getType()).thenReturn(0L);
        when(team.getTitle()).thenReturn("Deleted Project");

        alertCreateService.saveTeamDeleteAlert(sendTo, team);

        verify(alertRepository, times(sendTo.size())).save(any(Alert.class));
    }

    @Test
    void testSaveTeamUserRecruitAlert() {
        User leader = mock(User.class);
        User volunteer = mock(User.class);
        Team team = mock(Team.class);
        Long recruitID = 1L;

        when(team.getType()).thenReturn(0L);
        when(team.getTitle()).thenReturn("Recruit Project");
        when(volunteer.getNickname()).thenReturn("Volunteer");

        alertCreateService.saveTeamUserRecruitAlert(leader, volunteer, team, recruitID);

        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void testSaveUserAcceptedToTeamAlert() {
        List<User> sendTo = List.of(mock(User.class), mock(User.class));
        TeamUser volunteer = mock(TeamUser.class);
        AcceptFormRequest acceptForm = mock(AcceptFormRequest.class);
        Team team = mock(Team.class);
        User user = mock(User.class);

        when(volunteer.getTeam()).thenReturn(team);
        when(volunteer.getUser()).thenReturn(user); // volunteer의 getUser() 반환값 설정
        when(user.getNickname()).thenReturn("Volunteer"); // user의 getNickname() 반환값 설정
        when(team.getType()).thenReturn(0L);
        when(team.getTitle()).thenReturn("Project");
        when(acceptForm.getRole()).thenReturn(RoleType.AI);

        alertCreateService.saveUserAcceptedToTeamAlert(sendTo, volunteer, acceptForm);

        verify(alertRepository, times(sendTo.size())).save(any(Alert.class));
    }

    @Test
    void testSaveUserRefusedToTeamAlert() {
        TeamUser leader = mock(TeamUser.class);
        User volunteer = mock(User.class);
        Long refuseID = 1L;
        Team team = mock(Team.class);

        when(leader.getTeam()).thenReturn(team);
        when(team.getType()).thenReturn(0L);
        when(team.getTitle()).thenReturn("Refused Project");
        when(volunteer.getNickname()).thenReturn("Volunteer");

        alertCreateService.saveUserRefusedToTeamAlert(leader, volunteer, refuseID);

        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void testSaveUserKickedToTeamAlert() {
        TeamUser kickedUser = mock(TeamUser.class);
        Long refuseID = 1L;
        Team team = mock(Team.class);

        when(kickedUser.getTeam()).thenReturn(team);
        when(team.getType()).thenReturn(0L);
        when(team.getTitle()).thenReturn("Kicked Project");

        alertCreateService.saveUserKickedToTeamAlert(kickedUser, refuseID);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testSaveFeedbackAlert() {
        User giver = mock(User.class);
        User receiver = mock(User.class);
        Team team = mock(Team.class);

        when(team.getType()).thenReturn(0L);
        when(team.getTitle()).thenReturn("Feedback Project");
        when(receiver.getNickname()).thenReturn("Receiver");

        alertCreateService.saveFeedbackAlert(giver, receiver, team);

        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void testSaveCommentCreateAlert() {
        Feed feed = mock(Feed.class);
        User commenter = mock(User.class);
        Comment comment = mock(Comment.class);

        when(commenter.getNickname()).thenReturn("Commenter");
        when(comment.getContent()).thenReturn("Comment Content");
        when(feed.getUser()).thenReturn(mock(User.class));

        alertCreateService.saveCommentCreateAlert(feed, commenter, comment);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testSaveFeedLikeAlert() {
        User liker = mock(User.class);
        Feed feed = mock(Feed.class);
        Integer likes = 10;

        when(liker.getNickname()).thenReturn("Liker");
        when(feed.getTitle()).thenReturn("Feed Title");
        when(feed.getUser()).thenReturn(mock(User.class));

        alertCreateService.saveFeedLikeAlert(liker, feed, likes);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testSaveTeamLikeAlert() {
        User liker = mock(User.class);
        Team team = mock(Team.class);
        Integer likes = 20;

        when(liker.getNickname()).thenReturn("Liker");
        when(team.getTitle()).thenReturn("Team Title");
        when(team.getTeamUserList()).thenReturn(List.of(mock(TeamUser.class), mock(TeamUser.class)));

        alertCreateService.saveTeamLikeAlert(liker, team, likes);

        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void testSaveUserLikeAlert() {
        User liker = mock(User.class);
        User receiver = mock(User.class);
        Long likes = 30L;

        when(liker.getNickname()).thenReturn("Liker");
        when(receiver.getNickname()).thenReturn("Receiver");

        alertCreateService.saveUserLikeAlert(liker, receiver, likes);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testEnterpriseVerifyAlert() {
        User user = mock(User.class);
        boolean isEnterprise = true;

        alertCreateService.enterpriseVerifyAlert(user, isEnterprise);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }
}