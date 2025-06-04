package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.Member;
import com.example.matchup.matchupbackend.dto.request.team.TeamCreateRequest;
import com.example.matchup.matchupbackend.dto.request.team.TeamSearchRequest;
import com.example.matchup.matchupbackend.dto.response.mentoring.MentoringSearchResponse;
import com.example.matchup.matchupbackend.dto.response.team.*;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.error.exception.DuplicateEx.DuplicateFeedEx.DuplicateLikeException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.TeamDetailNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.TeamNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.repository.*;
import com.example.matchup.matchupbackend.repository.mentoring.MentoringRepository;
import com.example.matchup.matchupbackend.repository.mentoring.ReviewMentoringRepository;
import com.example.matchup.matchupbackend.repository.mentoring.TeamMentoringRepository;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeamServiceMockTest {

    private TeamService teamService;
    private TeamRepository teamRepository;
    private TeamUserRepository teamUserRepository;
    private TeamTagRepository teamTagRepository;
    private UserRepository userRepository;
    private TagRepository tagRepository;
    private TeamPositionRepository teamPositionRepository;
    private FileService fileService;
    private AlertCreateService alertCreateService;
    private TokenProvider tokenProvider;
    private LikeRepository likeRepository;
    private TeamMentoringRepository teamMentoringRepository;
    private ReviewMentoringRepository reviewMentoringRepository;
    private MentoringRepository mentoringRepository;

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        teamUserRepository = mock(TeamUserRepository.class);
        teamTagRepository = mock(TeamTagRepository.class);
        userRepository = mock(UserRepository.class);
        tagRepository = mock(TagRepository.class);
        teamPositionRepository = mock(TeamPositionRepository.class);
        fileService = mock(FileService.class);
        alertCreateService = mock(AlertCreateService.class);
        tokenProvider = mock(TokenProvider.class);
        likeRepository = mock(LikeRepository.class);
        teamMentoringRepository = mock(TeamMentoringRepository.class);
        reviewMentoringRepository = mock(ReviewMentoringRepository.class);
        mentoringRepository = mock(MentoringRepository.class);
        teamService = new TeamService(teamRepository, teamUserRepository, teamTagRepository, userRepository, tagRepository,
                teamPositionRepository, fileService, alertCreateService, tokenProvider, likeRepository, teamMentoringRepository,
                reviewMentoringRepository, mentoringRepository);
    }

    @Test
    void searchSliceTeamResponseList_정상반환() {
        TeamSearchRequest req = TeamSearchRequest.builder()
                .type(0L)
                .category("웹")
                .search("팀")
                .page(0)
                .size(10)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // 팀 생성
        TeamCreateRequest createReq = createTeamCreateRequest("팀A");
        Team teamA = Team.of(1L, createReq);
        Team teamB = Team.of(1L, createTeamCreateRequest("팀B"));
        List<Team> teams = Arrays.asList(teamA, teamB);
        Slice<Team> slice = new SliceImpl<>(teams, pageable, true);

        // userRepository 모킹: id가 1인 사용자 포함
        User leader = User.createUserForTest();
        org.springframework.test.util.ReflectionTestUtils.setField(leader, "id", 1L);
        when(userRepository.findAllUser()).thenReturn(Arrays.asList(leader));

        when(teamRepository.findTeamSliceByTeamRequest(any(), any())).thenReturn(slice);
        SliceTeamResponse result = teamService.searchSliceTeamResponseList(req, pageable);
        assertEquals(teams.size(), result.getTeamSearchResponseList().size());
    }

    @Test
    void teamSearchResponseList_정상변환() {
        User user = User.createUserForTest();
        when(userRepository.findAllUser()).thenReturn(Arrays.asList(user));
        TeamCreateRequest createReq = createTeamCreateRequest("검색팀");
        Team team = Team.of(user.getId(), createReq);
        List<TeamSearchResponse> responses = teamService.teamSearchResponseList(Collections.singletonList(team));
        assertThat(responses).hasSize(1);
    }

    @Test
    void getUserMap_모든유저정보맵으로반환() {
        User userA = User.createUserForTest();
        User userB = User.createUserForTest();
        when(userRepository.findAllUser()).thenReturn(Arrays.asList(userA, userB));
        Map<Long, User> userMap = teamService.getUserMap();
        assertEquals(1, userMap.size());
    }

    @Test
    void makeNewTeam_리더존재하면_정상생성() {
        Long leaderId = 10L;
        TeamCreateRequest createReq = spy(createTeamCreateRequest("새팀"));
        doReturn(null).when(createReq).getImageBase64();
        doReturn(Collections.emptyList()).when(createReq).getMemberList();
        User leader = User.createUserForTest();
        when(userRepository.findById(leaderId)).thenReturn(Optional.of(leader));
        Team savedTeam = Team.of(leaderId, createReq);
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);
        when(teamUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Long resultId = teamService.makeNewTeam(leaderId, createReq);
        assertEquals(savedTeam.getId(), resultId);
        verify(alertCreateService, times(1)).saveTeamCreateAlert(eq(resultId), eq(leader), eq(createReq));
    }

    @Test
    void makeNewTeam_리더없으면_UserNotFoundException발생() {
        Long leaderId = 20L;
        TeamCreateRequest createReq = createTeamCreateRequest("팀X");
        when(userRepository.findById(leaderId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> teamService.makeNewTeam(leaderId, createReq));
    }

    @Test
    void updateTeam_정상업데이트() {
        Long leaderId = 1L;
        Long teamId = 50L;
        TeamCreateRequest createReq = createTeamCreateRequest("업데이트팀");
        Team team = Team.of(leaderId, createReq);
        team = spy(team);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(teamPositionRepository.findByTeam(team)).thenReturn(Collections.emptyList());
        Long updatedId = teamService.updateTeam(leaderId, teamId, createReq);
        assertEquals(teamId, updatedId);
        verify(alertCreateService, times(1)).saveTeamUpdateAlert(eq(teamId), anyList(), eq(createReq));
    }

    @Test
    void deleteTeam_정상삭제() {
        Long leaderId = 1L;
        Long teamId = 60L;
        TeamCreateRequest createReq = createTeamCreateRequest("삭제팀");
        Team team = spy(Team.of(leaderId, createReq));
        when(team.getThumbnailUploadUrl()).thenReturn("dummyUrl");
        when(team.getThumbnailUrl()).thenReturn("dummyUrl");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        doNothing().when(fileService).deleteImage(anyString());
        doNothing().when(alertCreateService).saveTeamDeleteAlert(anyList(), eq(team));
        teamService.deleteTeam(leaderId, teamId);
        verify(fileService, times(1)).deleteImage("dummyUrl");
        verify(alertCreateService, times(1)).saveTeamDeleteAlert(anyList(), eq(team));
    }

    @Test
    void getTeamInfo_존재하면_정상반환() {
        Long teamId = 70L;
        // 가짜 TeamDetailResponse 객체 사용
        TeamDetailResponse detail = new TeamDetailResponse();
        when(teamRepository.findTeamInfoByTeamId(teamId)).thenReturn(detail);
        TeamDetailResponse result = teamService.getTeamInfo(teamId);
        assertSame(detail, result);
    }

    @Test
    void getTeamInfo_없으면_TeamDetailNotFoundException발생() {
        Long teamId = 80L;
        when(teamRepository.findTeamInfoByTeamId(teamId)).thenReturn(null);
        assertThrows(TeamDetailNotFoundException.class, () -> teamService.getTeamInfo(teamId));
    }

    @Test
    void getTeamMeetingSpot_존재하면_정상반환() {
        Long teamId = 90L;
        MeetingSpotResponse response = MeetingSpotResponse.builder()
                .onOffline("ONLINE")
                .city("서울")
                .detailSpot("강남")
                .build();
        when(teamRepository.findMeetingSpotByTeamId(teamId)).thenReturn(response);
        MeetingSpotResponse result = teamService.getTeamMeetingSpot(teamId);
        assertSame(response, result);
    }


    @Test
    void getTeamMentoringCardList_정상반환() {
        Long teamId = 100L;
        User mentor = User.createUserForTest(); // mentor 필드 할당
        Mentoring mentoring = Mentoring.builder()
                .mentor(mentor)
                .build();
        TeamMentoring teamMentoring = TeamMentoring.builder()
                .status(ApplyStatus.WAITING)
                .mentoring(mentoring)
                .team(Team.of(1L, createTeamCreateRequest("팀명")))
                .build();
        List<TeamMentoring> list = Collections.singletonList(teamMentoring);
        when(teamRepository.findTeamMentoringListByTeamId(teamId)).thenReturn(list);
        List<MentoringCardResponse> responses = teamService.getTeamMentoringCardList(teamId);
        assertEquals(1, responses.size());
    }

    @Test
    void getTeamTagStringList_정상반환() {
        Long teamId = 110L;
        when(teamRepository.findTeamTagByTeamId(teamId)).thenReturn(Collections.emptyList());
        List<String> tags = teamService.getTeamTagStringList(teamId);
        assertThat(tags).isEmpty();
    }

    @Test
    void getTeamType_정상반환() {
        Long teamId = 120L;
        TeamTypeResponse typeResponse = TeamTypeResponse.builder()
                .teamType(0L)
                .detailType("개발")
                .build();
        TeamCreateRequest req = createTeamCreateRequest("타입팀");
        Team team = Team.of(1L, req);
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        try (MockedStatic<TeamTypeResponse> mocked = Mockito.mockStatic(TeamTypeResponse.class)) {
            mocked.when(() -> TeamTypeResponse.fromTeamEntity(team)).thenReturn(typeResponse);
            TeamTypeResponse result = teamService.getTeamType(teamId);
            assertSame(typeResponse, result);
        }
    }

    @Test
    void getTeamType_삭제된팀이면_TeamNotFoundException발생() {
        Long teamId = 130L;
        TeamCreateRequest req = createTeamCreateRequest("삭제팀");
        // 삭제된 팀으로 간주
        // 팀 객체의 isDeleted 값를 변경할 수 없으므로 teamRepository.findTeamById()에서 Optional.empty()를 반환하도록 모킹
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.empty());
        assertThrows(TeamNotFoundException.class, () -> teamService.getTeamType(teamId));
    }

    @Test
    void getTeamLikes_토큰없으면_정상반환() {
        Long teamId = 140L;
        Team team = Team.of(1L, createTeamCreateRequest("좋아요팀"));
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        // team.getLikes()에서 size 반환 (빈 리스트)
        TeamLikeResponse response = teamService.getTeamLikes(null, teamId);
        assertThat(response.getTotalLike()).isEqualTo(team.getLikes().size());
        assertThat(response.getCheck()).isFalse();
    }

    @Test
    void likeTeam_정상처리() {
        String token = "Bearer token";
        Long teamId = 150L;
        Long userId = 1L;
        User user = User.createUserForTest();
        Team team = Team.of(userId, createTeamCreateRequest("좋아요팀"));
        when(tokenProvider.getUserId(eq(token), anyString())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teamRepository.findTeamJoinTeamUserById(teamId)).thenReturn(Optional.of(team));
        when(likeRepository.existsLikeByTeamAndUser(team, user)).thenReturn(false);
        when(likeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Long resultUserId = teamService.likeTeam(token, teamId);
        assertEquals(userId, resultUserId);
    }

    @Test
    void likeTeam_이미좋아요_DuplicateLikeException발생() {
        String token = "Bearer token";
        Long teamId = 160L;
        Long userId = 1L;
        User user = User.createUserForTest();
        Team team = Team.of(userId, createTeamCreateRequest("중복좋아요팀"));
        when(tokenProvider.getUserId(eq(token), anyString())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teamRepository.findTeamJoinTeamUserById(teamId)).thenReturn(Optional.of(team));
        when(likeRepository.existsLikeByTeamAndUser(team, user)).thenReturn(true);
        assertThrows(DuplicateLikeException.class, () -> teamService.likeTeam(token, teamId));
    }

    @Test
    void undoLikeTeam_정상처리() {
        String token = "Bearer token";
        Long teamId = 170L;
        Long userId = 1L;
        User user = User.createUserForTest();
        Team team = Team.of(userId, createTeamCreateRequest("취소좋아요팀"));
        when(tokenProvider.getUserId(eq(token), anyString())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        Likes like = Likes.builder()
                .user(user)
                .team(team)
                .likeReceiver(null)
                .feed(null)
                .mentoring(null)
                .jobPosting(null)
                .build();
        when(likeRepository.findLikesByTeamAndUser(team, user)).thenReturn(Optional.of(like));
        Long resultUserId = teamService.undoLikeTeam(token, teamId);
        assertEquals(userId, resultUserId);
    }

    @Test
    void showMentoringsInTeamPage_정상처리() {
        String token = "Bearer token";
        Long teamId = 180L;
        Long userId = 1L;
        User user = User.createUserForTest();
        Team team = Team.of(userId, createTeamCreateRequest("멘토링팀"));
        when(tokenProvider.getUserId(anyString())).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teamRepository.findTeamById(teamId)).thenReturn(Optional.of(team));
        User mentor = User.createUserForTest();
        Mentoring mentoring = Mentoring.builder()
                .mentor(mentor)
                .build();
        TeamMentoring teamMentoring = TeamMentoring.builder()
                .status(ApplyStatus.WAITING)
                .mentoring(mentoring)
                .team(team)
                .build();
        teamMentoring = spy(teamMentoring);
        doReturn(mentoring).when(teamMentoring).getMentoring();
        when(teamMentoringRepository.findAllDistinctByTeamAndTeamMentoringStatusIn(eq(team), anyList()))
                .thenReturn(Collections.singletonList(teamMentoring));
        List<MentoringSearchResponse> responses = teamService.showMentoringsInTeamPage(token, teamId);
        assertThat(responses).isNotEmpty();
    }

    @Test
    void finishTeam_정상종료() {
        String token = "finishToken";
        Long teamId = 190L;
        Long leaderId = 300L;
        Team team = Team.of(leaderId, createTeamCreateRequest("종료팀"));
        team = spy(team);
        when(tokenProvider.getUserId(token, "finishTeam")).thenReturn(leaderId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        teamService.finishTeam(token, teamId);
        verify(team, times(1)).finishTeam();
    }

    private TeamCreateRequest createTeamCreateRequest(String name) {
        return TeamCreateRequest.builder()
                .imageBase64(null)
                .imageName(null)
                .name(name)
                .type(TeamTypeResponse.builder().teamType(0L).detailType("웹").build())
                .description("설명")
                .meetingSpot(MeetingSpotResponse.builder().onOffline("ONLINE").city("서울").detailSpot("강남").build())
                .meetingDate("2021-08-01")
                .memberList(Arrays.asList(
                        Member.builder().role("BE").stacks(Arrays.asList("java", "spring")).maxCount(3L).build()
                ))
                .build();
    }
}