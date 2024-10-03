package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.UserCardResponse;
import com.example.matchup.matchupbackend.dto.response.team.SliceTeamResponse;
import com.example.matchup.matchupbackend.dto.response.team.TeamSearchResponse;
import com.example.matchup.matchupbackend.dto.response.user.SliceUserCardResponse;
import com.example.matchup.matchupbackend.dto.response.user.UserLikeResponse;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.error.exception.InvalidValueEx.InvalidLikeException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.LikeNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.repository.LikeRepository;
import com.example.matchup.matchupbackend.repository.team.TeamRepository;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import com.example.matchup.matchupbackend.repository.UserPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LikeServiceTest {

    private LikeRepository likeRepository;
    private AlertCreateService alertCreateService;
    private UserRepository userRepository;
    private TeamRepository teamRepository;
    private UserPositionRepository userPositionRepository;
    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        alertCreateService = mock(AlertCreateService.class);
        userRepository = mock(UserRepository.class);
        teamRepository = mock(TeamRepository.class);
        userPositionRepository = mock(UserPositionRepository.class);
        likeService = new LikeService(likeRepository, alertCreateService, userRepository, teamRepository, userPositionRepository);
    }

    @Test
    void 유저에게_좋아요_정상_저장() {
        Long giverId = 1L, receiverId = 2L;
        User giver = mock(User.class);
        User receiver = mock(User.class);

        when(likeRepository.existsByUserIdAndAndLikeReceiverId(giverId, receiverId)).thenReturn(false);
        when(userRepository.findById(giverId)).thenReturn(Optional.of(giver));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(receiver.getLikes()).thenReturn(7L);

        likeService.saveLikeToUser(giverId, receiverId);

        verify(receiver).addLike();
        verify(likeRepository).save(any(Likes.class));
        verify(alertCreateService).saveUserLikeAlert(giver, receiver, 7L);
    }

    @Test
    void 유저에게_자기자신_좋아요_예외() {
        assertThrows(InvalidLikeException.class, () -> likeService.saveLikeToUser(1L, 1L));
    }

    @Test
    void 유저에게_중복_좋아요_예외() {
        when(likeRepository.existsByUserIdAndAndLikeReceiverId(1L, 2L)).thenReturn(true);
        assertThrows(InvalidLikeException.class, () -> likeService.saveLikeToUser(1L, 2L));
    }

    @Test
    void 유저에게_좋아요_주는유저없음_예외() {
        when(likeRepository.existsByUserIdAndAndLikeReceiverId(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> likeService.saveLikeToUser(1L, 2L));
    }

    @Test
    void 유저에게_좋아요_받는유저없음_예외() {
        User giver = mock(User.class);
        when(likeRepository.existsByUserIdAndAndLikeReceiverId(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(giver));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> likeService.saveLikeToUser(1L, 2L));
    }

    @Test
    void 유저좋아요_삭제_정상() {
        Long giverId = 1L, receiverId = 2L;
        Likes likes = mock(Likes.class);
        User receiver = mock(User.class);

        when(likeRepository.findByUserIdAndLikeReceiverId(giverId, receiverId)).thenReturn(Optional.of(likes));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));

        likeService.deleteLikeToUser(giverId, receiverId);

        verify(receiver).deleteLike();
        verify(likeRepository).delete(likes);
    }

    @Test
    void 유저좋아요_삭제_좋아요없음_예외() {
        when(likeRepository.findByUserIdAndLikeReceiverId(1L, 2L)).thenReturn(Optional.empty());
        assertThrows(LikeNotFoundException.class, () -> likeService.deleteLikeToUser(1L, 2L));
    }

    @Test
    void 유저좋아요_삭제_유저없음_예외() {
        Likes likes = mock(Likes.class);
        when(likeRepository.findByUserIdAndLikeReceiverId(1L, 2L)).thenReturn(Optional.of(likes));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> likeService.deleteLikeToUser(1L, 2L));
    }

    @Test
    void 좋아요한_프로젝트_조회_정상() {
        Long userId = 1L;
        Likes like = mock(Likes.class);
        Team team = mock(Team.class);
        when(like.getTeam()).thenReturn(team);
        when(team.getId()).thenReturn(10L);
        when(likeRepository.findLikesJoinProjectByUserId(userId)).thenReturn(List.of(like));
        Pageable pageable = PageRequest.of(0, 10);
        when(teamRepository.findAllByIdIn(List.of(10L), pageable)).thenReturn(new SliceImpl<>(List.of(team), pageable, false));
        when(userRepository.findAllUser()).thenReturn(List.of());
        try (MockedStatic<TeamSearchResponse> staticMock = mockStatic(TeamSearchResponse.class)) {
            TeamSearchResponse tsr = mock(TeamSearchResponse.class);
            staticMock.when(() -> TeamSearchResponse.from(any(Team.class), anyMap())).thenReturn(tsr);
            SliceTeamResponse response = likeService.getLikedSliceProjectTeamResponse(userId, pageable);
            assertEquals(1, response.getTeamSearchResponseList().size());
        }
    }

    @Test
    void 좋아요한_유저_조회_정상() {
        Long userId = 1L;
        User receiver = mock(User.class);
        Likes like = mock(Likes.class);
        when(like.getLikeReceiver()).thenReturn(receiver);
        when(receiver.getId()).thenReturn(2L);
        when(likeRepository.findLikesJoinUserByUserId(userId)).thenReturn(List.of(like));
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAllByIdIn(List.of(2L), pageable)).thenReturn(new SliceImpl<>(List.of(receiver), pageable, false));
        UserPosition up = mock(UserPosition.class);
        when(userPositionRepository.findAllByUser(receiver)).thenReturn(List.of(up));
        when(up.getTypeLevel()).thenReturn(1);
        when(up.getId()).thenReturn(1L);
        try (MockedStatic<UserCardResponse> staticMock = mockStatic(UserCardResponse.class)) {
            UserCardResponse ucr = mock(UserCardResponse.class);
            staticMock.when(() -> UserCardResponse.fromEntity(eq(receiver), any())).thenReturn(ucr);
            SliceUserCardResponse response = likeService.getLikedSliceUserCardResponse(userId, pageable);
            assertEquals(1, response.getUserCardResponses().size());
        }
    }

    @Test
    void 유저_좋아요_여부_로그인_존재() {
        Long userId = 1L, receiverId = 2L;
        User receiver = mock(User.class);
        Likes like = mock(Likes.class);
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(likeRepository.findByUserIdAndLikeReceiver(userId, receiver)).thenReturn(Optional.of(like));
        when(receiver.getLikes()).thenReturn(3L);

        UserLikeResponse response = likeService.checkUserLiked(userId, receiverId);

        assertTrue(response.getCheck());
        assertEquals(3, response.getTotalLike());
    }

    @Test
    void 유저_좋아요_여부_로그인_없음() {
        Long userId = 1L, receiverId = 2L;
        User receiver = mock(User.class);
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(likeRepository.findByUserIdAndLikeReceiver(userId, receiver)).thenReturn(Optional.empty());
        when(receiver.getLikes()).thenReturn(2L);

        UserLikeResponse response = likeService.checkUserLiked(userId, receiverId);

        assertFalse(response.getCheck());
        assertEquals(2, response.getTotalLike());
    }

    @Test
    void 유저_좋아요_여부_비로그인() {
        Long receiverId = 2L;
        User receiver = mock(User.class);
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(receiver.getLikes()).thenReturn(1L);

        UserLikeResponse response = likeService.checkUserLiked(null, receiverId);

        assertFalse(response.getCheck());
        assertEquals(1, response.getTotalLike());
    }

    @Test
    void 유저_좋아요_여부_유저없음_예외() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> likeService.checkUserLiked(1L, 2L));
    }
}