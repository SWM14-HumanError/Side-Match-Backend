package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.TestSecurityConfig;
import com.example.matchup.matchupbackend.dto.request.feed.FeedCommentCreateOrUpdateRequest;
import com.example.matchup.matchupbackend.dto.request.feed.FeedCreateOrUpdateRequest;
import com.example.matchup.matchupbackend.dto.request.feed.FeedSearchRequest;
import com.example.matchup.matchupbackend.dto.response.feed.FeedCommentSliceResponse;
import com.example.matchup.matchupbackend.dto.response.feed.FeedSliceResponse;
import com.example.matchup.matchupbackend.entity.Comment;
import com.example.matchup.matchupbackend.entity.Feed;
import com.example.matchup.matchupbackend.service.FeedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
@WithMockUser
@WebMvcTest(controllers = FeedController.class)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedService feedService;

    private static final String AUTH_HEADER = "Bearer testtoken";

    @Test
    @DisplayName("피드 목록 조회 성공")
    void showFeeds_success() throws Exception {
        Mockito.when(feedService.getSliceFeed(any(FeedSearchRequest.class), any(Pageable.class), anyString()))
                .thenReturn(Mockito.mock(FeedSliceResponse.class));

        mockMvc.perform(get("/api/v1/feeds")
                        .param("keyword", "테스트")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("피드 생성 성공")
    void createFeed_success() throws Exception {
        FeedCreateOrUpdateRequest request = new FeedCreateOrUpdateRequest();
        Feed feed = Mockito.mock(Feed.class);
        Mockito.when(feed.getId()).thenReturn(1L);
        Mockito.when(feedService.saveFeed(any(FeedCreateOrUpdateRequest.class), anyString())).thenReturn(feed);

        String requestJson = "{"
                + "\"title\": \"생성된 피드\","
                + "\"content\": \"생성 내용\","
                + "\"type\": 0,"
                + "\"domain\": \"전체\","
                + "\"imageName\": \"test.png\","
                + "\"imageBase64\": \"base64string\""
                + "}";
        mockMvc.perform(post("/api/v1/feed")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("피드 수정 성공")
    void updateFeed_success() throws Exception {
        Feed feed = Mockito.mock(Feed.class);
        Mockito.when(feed.getId()).thenReturn(1L);
        Mockito.when(feedService.updateFeed(any(FeedCreateOrUpdateRequest.class), anyString(), anyLong())).thenReturn(feed);

        String requestJson = "{"
                + "\"title\": \"수정된 피드\","
                + "\"content\": \"수정 내용\","
                + "\"type\": 0,"
                + "\"domain\": \"전체\","
                + "\"imageName\": \"test.png\","
                + "\"imageBase64\": \"base64string\""
                + "}";

        mockMvc.perform(put("/api/v1/feed/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("피드 삭제 성공")
    void deleteFeed_success() throws Exception {
        Mockito.when(feedService.deleteFeed(anyString(), anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/v1/feed/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("피드 댓글 생성 성공")
    void createFeedComment_success() throws Exception {
        Comment comment = Mockito.mock(Comment.class);
        Mockito.when(comment.getId()).thenReturn(1L);
        Mockito.when(feedService.createFeedComment(anyString(), anyLong(), any(FeedCommentCreateOrUpdateRequest.class))).thenReturn(comment);

        String requestJson = "{ \"content\": \"댓글 내용\" }";

        mockMvc.perform(post("/api/v1/feed/1/comment")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("피드 댓글 목록 조회 성공")
    void showFeedComments_success() throws Exception {
        Mockito.when(feedService.getSliceFeedComments(anyLong(), any(Pageable.class)))
                .thenReturn(Mockito.mock(FeedCommentSliceResponse.class));

        mockMvc.perform(get("/api/v1/feed/1/comment"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("피드 댓글 수정 성공")
    void updateFeedComment_success() throws Exception {
        Comment comment = Mockito.mock(Comment.class);
        Mockito.when(feedService.updateFeedComment(anyString(), anyLong(), anyLong(), any(FeedCommentCreateOrUpdateRequest.class))).thenReturn(comment);

        String requestJson = "{ \"content\": \"수정된 댓글\" }";

        mockMvc.perform(put("/api/v1/feed/1/comment/1")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("피드 댓글 삭제 성공")
    void deleteFeedComment_success() throws Exception {
        mockMvc.perform(delete("/api/v1/feed/1/comment/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("피드 좋아요 추가 성공")
    void addFeedLike_success() throws Exception {
        Mockito.when(feedService.likeFeed(anyString(), anyLong())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/feed/1/like")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("피드 좋아요 취소 성공")
    void deleteFeedLike_success() throws Exception {
        Mockito.when(feedService.undoLikeFeed(anyString(), anyLong())).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/feed/1/like")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("피드 좋아요 개수 조회 성공")
    void checkFeedLike_success() throws Exception {
        Mockito.when(feedService.getFeedLikes(anyLong())).thenReturn(5);

        mockMvc.perform(get("/api/v1/feed/1/like"))
                .andExpect(status().isOk());
    }
}