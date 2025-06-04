package com.example.matchup.matchupbackend.controller;

import com.example.matchup.matchupbackend.dto.request.feed.FeedCommentCreateOrUpdateRequest;
import com.example.matchup.matchupbackend.dto.request.feed.FeedCreateOrUpdateRequest;
import com.example.matchup.matchupbackend.dto.request.feed.FeedSearchRequest;
import com.example.matchup.matchupbackend.dto.response.feed.FeedCommentSliceResponse;
import com.example.matchup.matchupbackend.dto.response.feed.FeedSearchResponse;
import com.example.matchup.matchupbackend.dto.response.feed.FeedSliceResponse;
import com.example.matchup.matchupbackend.entity.Comment;
import com.example.matchup.matchupbackend.entity.Feed;
import com.example.matchup.matchupbackend.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static com.example.matchup.matchupbackend.global.config.jwt.TokenProvider.HEADER_AUTHORIZATION;

@Slf4j
@RequestMapping(value = "/api/v1")
@RequiredArgsConstructor
@RestController
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/feeds")
    public ResponseEntity<FeedSliceResponse> showFeeds (@Valid FeedSearchRequest feedSearchRequest,
                                                       @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String authorizationHeader,
                                                       @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        FeedSliceResponse response = feedService.getSliceFeed(feedSearchRequest, pageable, authorizationHeader);
        return (response != null)
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().build();
    }

    @PostMapping("/feed")
    public ResponseEntity<Void> createFeed(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                           @Valid @RequestBody FeedCreateOrUpdateRequest request) {
        Feed feedCreated = feedService.saveFeed(request, authorizationHeader);
        if (feedCreated != null) {
            log.info("id: {}의 피드가 생성되었습니다.", feedCreated.getId());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/feed/{feed_id}")
    public ResponseEntity<FeedSearchResponse> showDetailFeed(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader, @PathVariable("feed_id") Long feedId) {
        FeedSearchResponse response = feedService.getFeedDetail(authorizationHeader, feedId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/feed/{feed_id}")
    public ResponseEntity<Void> updateFeed(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                           @Valid @RequestBody FeedCreateOrUpdateRequest request,
                                           @PathVariable("feed_id") Long feedId) {
        Feed feedUpdated = feedService.updateFeed(request, authorizationHeader, feedId);
        if (feedUpdated != null) {
            log.info("id: {}의 피드가 수정되었습니다.", feedUpdated.getId());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/feed/{feed_id}")
    public ResponseEntity<Void> deleteFeed(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                           @PathVariable("feed_id") Long feedId) {
        if (feedService.deleteFeed(authorizationHeader, feedId)) {
            log.info("id: {}의 피드가 삭제되었습니다.", feedId);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/feed/{feed_id}/comment")
    public ResponseEntity<Void> createFeedComment(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                                  @PathVariable("feed_id") Long feedId,
                                                  @Valid @RequestBody FeedCommentCreateOrUpdateRequest request) {
        Comment comment = feedService.createFeedComment(authorizationHeader, feedId, request);
        if (comment != null) {
            log.info("피드 id: {}의 댓글 id: {}가 생성되었습니다.", feedId, comment.getId());
            return ResponseEntity.created(URI.create("/feed/comment")).build();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/feed/{feed_id}/comment")
    public ResponseEntity<FeedCommentSliceResponse> showFeedComments(@PathVariable("feed_id") Long feedId,
                                                                     @PageableDefault(page = 0, size = 3, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        FeedCommentSliceResponse response = feedService.getSliceFeedComments(feedId, pageable);
        return (response != null)
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().build();
    }

    @PutMapping("/feed/{feed_id}/comment/{comment_id}")
    public ResponseEntity<Void> updateFeedComment(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                                  @PathVariable("feed_id") Long feedId,
                                                  @PathVariable("comment_id") Long commentId,
                                                  @Valid @RequestBody FeedCommentCreateOrUpdateRequest request) {
        Comment comment = feedService.updateFeedComment(authorizationHeader, feedId, commentId, request);
        if (comment != null) {
            log.info("피드 id: {}의 댓글 id: {}가 수정되었습니다.", feedId, commentId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/feed/{feed_id}/comment/{comment_id}")
    public ResponseEntity<Void> deleteFeedComment(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                                  @PathVariable("comment_id") Long commentId,
                                                  @PathVariable("feed_id") Long feedId) {
        feedService.deleteFeedComment(authorizationHeader, feedId, commentId);
        log.info("피드 id: {}의 댓글 id: {}가 삭제되었습니다.", feedId, commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/feed/{feed_id}/like")
    public ResponseEntity<Void> addFeedLike(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                            @PathVariable("feed_id") Long feedId) {
        Long userId = feedService.likeFeed(authorizationHeader, feedId);
        log.info("user id: {}가 {} 피드의 좋아요를 눌렀습니다.", userId, feedId);
        return ResponseEntity.created(URI.create("/feed")).build();
    }

    @DeleteMapping("/feed/{feed_id}/like")
    public ResponseEntity<Void> deleteFeedLike(@RequestHeader(value = HEADER_AUTHORIZATION) String authorizationHeader,
                                            @PathVariable("feed_id") Long feedId) {
        Long userId = feedService.undoLikeFeed(authorizationHeader, feedId);
        log.info("user id: {}가 {} 피드의 좋아요를 취소했습니다.", userId, feedId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/feed/{feed_id}/like")
    @ResponseStatus(HttpStatus.OK)
    public int checkFeedLike(@PathVariable("feed_id") Long feedId) {
        return feedService.getFeedLikes(feedId);
    }


}
