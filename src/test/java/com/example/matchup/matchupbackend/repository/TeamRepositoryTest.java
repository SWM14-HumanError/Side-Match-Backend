package com.example.matchup.matchupbackend.repository;

import com.example.matchup.matchupbackend.TestQueryFactoryConfig;
import com.example.matchup.matchupbackend.entity.Team;
import com.example.matchup.matchupbackend.entity.TeamTag;
import com.example.matchup.matchupbackend.entity.TeamUser;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.team.TeamRepositoryCustomImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestQueryFactoryConfig.class, TeamRepositoryCustomImpl.class})
public class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    private Team team;


    @BeforeEach
    public void setUp() {
        team = Team.builder()
                .title("Test Team")
                .description("This is a test team")
                .type(1L)
                .detailType("detail")
                .thumbnailUploadUrl("uploadUrl")
                .thumbnailStoreUrl("storeUrl")
                .onOffline("online")
                .city("Seoul")
                .detailSpot("Spot")
                .meetingTime("2023-10-30")
                .recruitFinish("NF")
                .leaderID(1L)
                .build();

        TeamUser teamUser = TeamUser.builder()
                .team(team)
                .approve(true)
                .build();
        team.getTeamUserList().add(teamUser);
        team = teamRepository.save(team);
    }
    @Test
    public void testFindTeamById() {
        Optional<Team> found = teamRepository.findTeamById(team.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Team");
    }

    @Test
    public void testIsFinished() {
        // 초기에 isDeleted=0이면 false 반환
        boolean finished = teamRepository.isFinished(team.getId());
        assertThat(finished).isFalse();

        // deleteTeam을 호출하면 isDeleted가 1L로 변경됨
        team.deleteTeam();
        team = teamRepository.save(team);
        boolean updatedFinished = teamRepository.isFinished(team.getId());
        assertThat(updatedFinished).isTrue();
    }

    @Test
    public void testFindTeamJoinTeamUserById() {
        Optional<Team> found = teamRepository.findTeamJoinTeamUserById(team.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTeamUserList()).isNotNull();
    }

    @Test
    public void testFindAllByIdIn() {
        Slice<Team> slice = teamRepository.findAllByIdIn(Arrays.asList(team.getId()), PageRequest.of(0, 10));
        assertThat(slice.getContent()).hasSize(1);
    }

    @Test
    public void testFindByLeaderIDAndIsDeletedAndType() {
        List<Team> teams = teamRepository.findByLeaderIDAndIsDeletedAndType(1L, 0L, 1L);
        assertThat(teams).isNotEmpty();
    }

    @Test
    public void testFindTeamByIdAndIsDeleted() {
        Optional<Team> found = teamRepository.findTeamByIdAndIsDeleted(team.getId(), 0L);
        assertThat(found).isPresent();
    }

    @Test
    public void testFindActiveTeamByLeaderID() {
        List<Team> teams = teamRepository.findActiveTeamByLeaderID(1L);
        assertThat(teams).isNotEmpty();
    }

    @Test
    public void testFindTeamTagByTeamId() {
        TeamTag tag = TeamTag.builder()
                .team(team)
                .tagName("Tag1")
                .build();
        team.addTeamTagList(tag);
        team = teamRepository.save(team);

        List<TeamTag> tags = teamRepository.findTeamTagByTeamId(team.getId());
        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).getTagName()).isEqualTo("Tag1");
    }
}