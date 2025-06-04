package com.example.matchup.matchupbackend.repository;

import com.example.matchup.matchupbackend.TestQueryFactoryConfig;
import com.example.matchup.matchupbackend.entity.Role;
import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepositoryCustomImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({UserRepositoryCustomImpl.class, TestQueryFactoryConfig.class})
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user1, user2, user3;

    @BeforeEach
    public void setUp() {
        user1 = User.createUserForTest();
        user1 = User.builder()
                .nickname("nick1")
                .email("user1@test.com")
                .name("User One")
                .pictureUrl("pic1")
                .role(Role.USER)
                .isMentor(false)
                .agreeTermOfServiceId(100L)
                .build();
        ReflectionTestUtils.setField(user1, "refreshToken", "token1");

        user2 = User.builder()
                .nickname("nick2")
                .email("user2@test.com")
                .name("User Two")
                .pictureUrl("pic2")
                .role(Role.USER)
                .isMentor(false)
                .agreeTermOfServiceId(200L)
                .build();
        ReflectionTestUtils.setField(user2, "refreshToken", "token2");

        user3 = User.builder()
                .nickname("nick3")
                .email("user3@test.com")
                .name("User Three")
                .pictureUrl("pic3")
                .role(Role.USER)
                .isMentor(false)
                .agreeTermOfServiceId(300L)
                .build();
        ReflectionTestUtils.setField(user3, "refreshToken", "token3");

        userRepository.saveAll(Arrays.asList(user1, user2, user3));
    }

    @Test
    public void testFindByEmail() {
        Optional<User> found = userRepository.findByEmail("user1@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("nick1");
    }

    @Test
    public void testFindByRefreshToken() {
        Optional<User> found = userRepository.findByRefreshToken("token2");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("user2@test.com");
    }

    @Test
    public void testFindAllUser() {
        List<User> users = userRepository.findAllUser();
        assertThat(users).hasSize(3);
    }

    @Test
    public void testFindUserById() {
        Optional<User> found = userRepository.findUserById(user3.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("nick3");
    }

    @Test
    public void testFindUserByNicknameAndIdNot() {
        Optional<User> found = userRepository.findUserByNicknameAndIdNot("nick1", user1.getId() + 100L);
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("user1@test.com");
        found = userRepository.findUserByNicknameAndIdNot("nick1", user1.getId());
        assertThat(found).isNotPresent();
    }

    @Test
    public void testFindUserByNickname() {
        Optional<User> found = userRepository.findUserByNickname("nick2");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("user2@test.com");
    }

    @Test
    public void testFindAllByIdIn() {
        List<Long> idList = Arrays.asList(user1.getId(), user3.getId());
        Slice<User> slice = userRepository.findAllByIdIn(idList, PageRequest.of(0, 10));
        assertThat(slice.getContent()).hasSize(2);
    }

    @Test
    public void testFindNicknameById() {
        String nickname = userRepository.findNicknameById(user2.getId());
        assertThat(nickname).isEqualTo("nick2");
    }
}