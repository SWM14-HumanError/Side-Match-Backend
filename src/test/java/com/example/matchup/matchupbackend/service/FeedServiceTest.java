package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.FeedSearchType;
import com.example.matchup.matchupbackend.dto.request.feed.FeedCreateOrUpdateRequest;
import com.example.matchup.matchupbackend.dto.request.feed.FeedSearchRequest;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.error.exception.AuthorizeException;
import com.example.matchup.matchupbackend.error.exception.DuplicateEx.DuplicateFeedEx.DuplicateLikeException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.FeedNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.repository.LikeRepository;
import com.example.matchup.matchupbackend.repository.comment.CommentRepository;
import com.example.matchup.matchupbackend.repository.feed.FeedRepository;
import com.example.matchup.matchupbackend.repository.feed.FeedRepositoryCustom;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedServiceTest {

    private EntityManager em;
    private UserRepository userRepository;
    private FeedRepositoryCustom feedRepositoryCustom;
    private FeedRepository feedRepository;
    private TokenProvider tokenProvider;
    private CommentRepository commentRepository;
    private LikeRepository likeRepository;
    private AlertCreateService alertCreateService;
    private FileService fileService;
    private FeedService feedService;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        userRepository = mock(UserRepository.class);
        feedRepositoryCustom = mock(FeedRepositoryCustom.class);
        feedRepository = mock(FeedRepository.class);
        tokenProvider = mock(TokenProvider.class);
        commentRepository = mock(CommentRepository.class);
        likeRepository = mock(LikeRepository.class);
        alertCreateService = mock(AlertCreateService.class);
        fileService = mock(FileService.class);
        feedService = new FeedService(em, userRepository, feedRepositoryCustom, feedRepository, tokenProvider, commentRepository, null, likeRepository, alertCreateService, fileService);
    }

    @Test
    void saveFeed_success() {
        String token = "token";
        Long userId = 1L;
        User user = mock(User.class);
        FeedCreateOrUpdateRequest request = mock(FeedCreateOrUpdateRequest.class);
        Feed feed = mock(Feed.class);

        when(tokenProvider.getUserId(token, "createFeed 중에 훼손된 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(request.toEntity(user)).thenReturn(feed);
        when(request.getImageBase64()).thenReturn(null);
        when(feedRepository.save(feed)).thenReturn(feed);

        Feed result = feedService.saveFeed(request, token);

        assertEquals(feed, result);
        verify(feedRepository).save(feed);
    }

    @Test
    void saveFeed_userNotFound() {
        String token = "token";
        Long userId = 1L;
        FeedCreateOrUpdateRequest request = mock(FeedCreateOrUpdateRequest.class);

        when(tokenProvider.getUserId(token, "createFeed 중에 훼손된 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> feedService.saveFeed(request, token));
    }

    @Test
    void updateFeed_success() {
        String token = "token";
        Long userId = 1L;
        Long feedId = 2L;
        User user = mock(User.class);
        FeedCreateOrUpdateRequest request = mock(FeedCreateOrUpdateRequest.class);
        Feed feed = mock(Feed.class);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(tokenProvider.getUserId(token, "updateFeed 중에 훼손된 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feed.getUser()).thenReturn(user);
        when(feed.updateFeed(request)).thenReturn(feed);

        Feed result = feedService.updateFeed(request, token, feedId);

        assertEquals(feed, result);
    }

    @Test
    void updateFeed_unauthorized() {
        String token = "token";
        Long userId = 1L;
        Long feedId = 2L;
        User user = mock(User.class);
        User otherUser = mock(User.class);
        FeedCreateOrUpdateRequest request = mock(FeedCreateOrUpdateRequest.class);
        Feed feed = mock(Feed.class);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(tokenProvider.getUserId(token, "updateFeed 중에 훼손된 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feed.getUser()).thenReturn(otherUser);

        assertThrows(AuthorizeException.class, () -> feedService.updateFeed(request, token, feedId));
    }

    @Test
    void deleteFeed_success() {
        String token = "token";
        Long userId = 1L;
        Long feedId = 2L;
        User user = mock(User.class);
        Feed feed = mock(Feed.class);
        List<Comment> comments = List.of();

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(tokenProvider.getUserId(token, "deleteFeed 중에 훼손된 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feed.getUser()).thenReturn(user);
        when(commentRepository.findAllByFeed(feed)).thenReturn(comments);

        boolean result = feedService.deleteFeed(token, feedId);

        assertTrue(result);
        verify(feedRepository).delete(feed);
    }

    @Test
    void deleteFeed_notOwner() {
        String token = "token";
        Long userId = 1L;
        Long feedId = 2L;
        User user = mock(User.class);
        User otherUser = mock(User.class);
        Feed feed = mock(Feed.class);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(tokenProvider.getUserId(token, "deleteFeed 중에 훼손된 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feed.getUser()).thenReturn(otherUser);

        boolean result = feedService.deleteFeed(token, feedId);

        assertFalse(result);
        verify(feedRepository, never()).delete(feed);
    }

    @Test
    void likeFeed_success() {
        String token = "token";
        Long userId = 1L;
        Long feedId = 2L;
        User user = mock(User.class);
        Feed feed = mock(Feed.class);

        when(tokenProvider.getUserId(token, "피드의 좋아요를 반영하면서 유효하지 않은 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feedRepository.findFeedJoinUserById(feedId)).thenReturn(Optional.of(feed));
        when(likeRepository.existsLikeByFeedAndUser(feed, user)).thenReturn(false);
        when(feed.getUser()).thenReturn(mock(User.class));
        when(feed.getLikes()).thenReturn(List.of());

        Long result = feedService.likeFeed(token, feedId);

        assertEquals(userId, result);
        verify(likeRepository).save(any(Likes.class));
    }

    @Test
    void likeFeed_duplicate() {
        String token = "token";
        Long userId = 1L;
        Long feedId = 2L;
        User user = mock(User.class);
        Feed feed = mock(Feed.class);

        when(tokenProvider.getUserId(token, "피드의 좋아요를 반영하면서 유효하지 않은 토큰을 받았습니다.")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(feedRepository.findFeedJoinUserById(feedId)).thenReturn(Optional.of(feed));
        when(likeRepository.existsLikeByFeedAndUser(feed, user)).thenReturn(true);

        assertThrows(DuplicateLikeException.class, () -> feedService.likeFeed(token, feedId));
    }

    @Test
    void getFeedLikes_success() {
        Long feedId = 1L;
        Feed feed = mock(Feed.class);
        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(feed.getLikes()).thenReturn(List.of(mock(Likes.class), mock(Likes.class)));

        int result = feedService.getFeedLikes(feedId);

        assertEquals(2, result);
    }

    @Test
    void getFeedLikes_feedNotFound() {
        Long feedId = 1L;
        when(feedRepository.findById(feedId)).thenReturn(Optional.empty());

        assertThrows(FeedNotFoundException.class, () -> feedService.getFeedLikes(feedId));
    }
}