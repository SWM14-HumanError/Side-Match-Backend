package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.FeedSearchType;
import com.example.matchup.matchupbackend.dto.UploadFile;
import com.example.matchup.matchupbackend.dto.request.feed.FeedCommentCreateOrUpdateRequest;
import com.example.matchup.matchupbackend.dto.request.feed.FeedCreateOrUpdateRequest;
import com.example.matchup.matchupbackend.dto.request.feed.FeedSearchRequest;
import com.example.matchup.matchupbackend.dto.response.feed.FeedCommentResponse;
import com.example.matchup.matchupbackend.dto.response.feed.FeedCommentSliceResponse;
import com.example.matchup.matchupbackend.dto.response.feed.FeedSearchResponse;
import com.example.matchup.matchupbackend.dto.response.feed.FeedSliceResponse;
import com.example.matchup.matchupbackend.entity.*;
import com.example.matchup.matchupbackend.error.exception.AuthorizeException;
import com.example.matchup.matchupbackend.error.exception.DuplicateEx.DuplicateFeedEx.DuplicateLikeException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.CommentNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.FeedNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotPermitEx.ResourceNotPermitException;
import com.example.matchup.matchupbackend.global.config.jwt.TokenProvider;
import com.example.matchup.matchupbackend.repository.LikeRepository;
import com.example.matchup.matchupbackend.repository.comment.CommentRepository;
import com.example.matchup.matchupbackend.repository.comment.CommentRepositoryCustom;
import com.example.matchup.matchupbackend.repository.feed.FeedRepository;
import com.example.matchup.matchupbackend.repository.feed.FeedRepositoryCustom;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.example.matchup.matchupbackend.error.ErrorCode.COMMENT_NOT_FOUND;
import static com.example.matchup.matchupbackend.error.ErrorCode.FEED_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Getter
@Slf4j
public class FeedService {

    private final EntityManager em;
    private final UserRepository userRepository;
    private final FeedRepositoryCustom feedRepositoryCustom;
    private final FeedRepository  feedRepository;
    private final TokenProvider tokenProvider;
    private final CommentRepository commentRepository;
    private final CommentRepositoryCustom commentRepositoryCustom;
    private final LikeRepository likeRepository;
    private final AlertCreateService alertCreateService;
    private final FileService fileService;

    /**
     * 유저가 로그인을 한 경우라면 유저가 표시한 좋아요 여부를 같이 응답
     * 아니라면 도메인, 작성자 혹은 피드 제목을 기준으로 필터된 값을 슬라이스하여 응답
     */
    public FeedSliceResponse getSliceFeed(FeedSearchRequest request, Pageable pageable, String authorizationHeader) {
        List<Feed> feeds = feedRepositoryCustom.findFeedListByFeedRequest(request, pageable);

        Long userId = tokenProvider.getUserId(authorizationHeader, "getSliceFeed");
        User user = (userId != null) ? userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("getSliceFeed")) : null;

        List<FeedSearchResponse> responseFeeds = new ArrayList<>();
        for (Feed feed : feeds) {
            FeedSearchResponse feedSearchResponse = FeedSearchResponse.fromEntity(feed);
            boolean isLiked = false;
            if (user != null) {
                isLiked = likeRepository.existsLikeByFeedAndUser(feed, user);
            }
            feedSearchResponse.setIsLiked(isLiked);
            responseFeeds.add(feedSearchResponse);
        }

        FeedSliceResponse response = new FeedSliceResponse();
        response.setFeedSearchResponses(responseFeeds);
        response.setSize(feeds.size());
        response.setHasNextSlice(hasNextFeedSlice(request, pageable));

        return response;
    }

    public FeedSearchResponse getFeedDetail(String authorizationHeader, Long feedId){
        tokenProvider.getUserId(authorizationHeader, "getFeedDetail");
        Feed feed = feedRepository.findFeedJoinUserById(feedId)
                .orElseThrow(FeedNotFoundException::new);
        return FeedSearchResponse.fromEntity(feed);
    }

    // todo: refactoring, hasNextSlice할 필요없이 하나의 query로 카운트 할 수 있음.
    private boolean hasNextFeedSlice(FeedSearchRequest request, Pageable pageable) {

        if (request.getSearchType() != null) {
            if (request.getDomain() == ProjectDomain.전체) {
                String jpql = (request.getSearchType() == FeedSearchType.TITLE)
                        ? "select count(cnt) from (select f.id AS cnt from Feed f where f.title like CONCAT('%', :searchValue, '%') GROUP BY f.id order by f.id DESC)"
                        : "select count(cnt) from (select f.id AS cnt from Feed f join f.user u on u.name like CONCAT('%', :searchValue, '%') GROUP BY f.id order by f.id DESC)";

                try {
                    Long countQuery = em.createQuery(jpql, Long.class)
                            .setParameter("searchValue", request.getSearchValue())
                            .getSingleResult();
                    return countQuery > ((pageable.getPageNumber() + 1L) * pageable.getPageSize());
                } catch (Exception e) {
                    return false;
                }
            } else {
                String jpql = (request.getSearchType() == FeedSearchType.TITLE)
                        ? "select count(cnt) from (select f.id AS cnt from Feed f where f.title like CONCAT('%', :searchValue, '%') and f.projectDomain = :domain GROUP BY f.id order by f.id DESC)"
                        : "select count(cnt) from (select f.id AS cnt from Feed f join f.user u on u.name like CONCAT('%', :searchValue, '%') and f.projectDomain = :domain GROUP BY f.id order by f.id DESC)";

                try {
                    Long countQuery = em.createQuery(jpql, Long.class)
                            .setParameter("domain", request.getDomain())
                            .setParameter("searchValue", request.getSearchValue())
                            .getSingleResult();
                    return countQuery > ((pageable.getPageNumber() + 1L) * pageable.getPageSize());
                } catch (Exception e) {
                    return false;
                }
            }
        } else {
            if (request.getDomain() == ProjectDomain.전체) {
                String jpql = "select count(cnt) from (select f.id AS cnt from Feed f GROUP BY f.id)";

                try {
                    Long countQuery = em.createQuery(jpql, Long.class)
                            .getSingleResult();
                    return countQuery > ((pageable.getPageNumber() + 1L) * pageable.getPageSize());
                } catch (Exception e) {
                    return false;
                }
            } else {
                String jpql = "select count(cnt) from (select f.id AS cnt from Feed f WHERE f.projectDomain = :domain GROUP BY f.id)";

                try {
                    Long countQuery = em.createQuery(jpql, Long.class)
                            .setParameter("domain", request.getDomain())
                            .getSingleResult();
                    return countQuery > ((pageable.getPageNumber() + 1L) * pageable.getPageSize());
                } catch (Exception e) {
                    return false;
                }
            }
        }
    }

    @Transactional
    public Feed saveFeed(FeedCreateOrUpdateRequest request, String authorizationHeader) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "createFeed 중에 훼손된 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("createFeed 중에 존재하지 않는 유저에 접근했습니다."));
        Feed feed = request.toEntity(user);
        if (request.getImageBase64() != null) { //썸네일 사진이 있는 경우
            UploadFile uploadFile = fileService.storeBase64ToFile(request.getImageBase64(), request.getImageName());
            feed.setUploadFile(uploadFile);
        }
        return feedRepository.save(feed);
    }

    @Transactional
    public Feed updateFeed(FeedCreateOrUpdateRequest request, String authorizationHeader, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new UserNotFoundException("updateFeed 중에 존재하지 않는 피드에 접근했습니다."));
        Long userId = tokenProvider.getUserId(authorizationHeader, "updateFeed 중에 훼손된 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("updateFeed 중에 존재하지 않는 유저에 접근했습니다."));

        if (feed.getUser().equals(user)) {
            feedThumbnailUpdate(feed, request);
            return feed.updateFeed(request);
        }
        else {
            throw new AuthorizeException("updateFeed 중에 인가받지 못한 유저의 접근입니다.");
        }
    }

    private void feedThumbnailUpdate(Feed feed, FeedCreateOrUpdateRequest request) {
        if(feed.getThumbnailUrl() != null){
            fileService.deleteImage(feed.getThumbnailUrl());
            feed.deleteImage();
        }
        if (request.getImageBase64() != null) { //썸네일 사진이 있는 경우
            UploadFile uploadFile = fileService.storeBase64ToFile(request.getImageBase64(), request.getImageName());
            feed.setUploadFile(uploadFile);
        }
    }

    @Transactional
    public boolean deleteFeed(String token, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new UserNotFoundException("deleteFeed 중에 존재하지 않는 피드에 접근했습니다."));
        Long userId = tokenProvider.getUserId(token, "deleteFeed 중에 훼손된 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("updateFeed 중에 존재하지 않는 유저에 접근했습니다."));
        List<Comment> commentsByFeed = commentRepository.findAllByFeed(feed);
        if (feed.getUser().equals(user)) {
            commentRepository.deleteAll(commentsByFeed);
            feedRepository.delete(feed);
            return true;
        }
        else {
            return false;
        }
    }

    @Transactional
    public Comment createFeedComment(String authorizationHeader, Long feedId, FeedCommentCreateOrUpdateRequest request) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "피드 댓글을 생성하는 과정에서 유효하지 않은 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("피드 댓글을 생성하는 과정에서 존재하지 않는 유저 id를 받았습니다."));
        Feed feed = feedRepository.findFeedJoinUserById(feedId).orElseThrow(() -> new FeedNotFoundException("피드 댓글을 생성하는 과정에서 존재하지 않는 피드 id를 받았습니다."));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .feed(feed)
                .user(user)
                .build();
        if (isNotFeedOfUser(user, feed)) {
            alertCreateService.saveCommentCreateAlert(feed, user, comment); // 댓글 작성 알림 생성
        }
        return commentRepository.save(comment);
    }

    @Transactional
    public Comment updateFeedComment(String authorizationHeader, Long feedId, Long commentId, FeedCommentCreateOrUpdateRequest request) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "피드 댓글을 업데이트하는 과정에서 유효하지 않은 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("피드 댓글을 업데이트하는 과정에서 존재하지 않는 유저 id를 받았습니다."));
        Feed feed = feedRepository.findFeedJoinUserById(feedId).orElseThrow(() -> new FeedNotFoundException("피드 댓글을 업데이트하는 과정에서 존재하지 않은 피드 id를 받았습니다."));
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new CommentNotFoundException("피드 댓글을 업데이트하는 과정에서 존재하지 않은 댓글 id를 받았습니다."));
        alertCreateService.saveCommentCreateAlert(feed, user, comment); // 댓글 작성 알림 생성
        if (comment.getUser().equals(user) && comment.getFeed().equals(feed)) {
            Comment updatedComment = comment.updateFeedComment(request);
            return commentRepository.save(updatedComment);
        } else {
            throw new ResourceNotPermitException(FEED_NOT_FOUND, "피드 댓글을 업데이트하는 과정에서 존재하지 않는 피드에 접근했거나 인가받지 못한 사용자로부터의 요청입니다.");
        }
    }

    @Transactional
    public void deleteFeedComment(String authorizationHeader, Long feedId, Long commentId) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "피드 댓글을 삭제하는 과정에서 유효하지 않은 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("피드 댓글을 삭제하는 과정에서 존재하지 않는 유저 id를 받았습니다."));
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new FeedNotFoundException("피드 댓글을 삭제하는 과정에서 존재하지 않은 피드 id를 받았습니다."));
        User feedOwner = feed.getUser();
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new CommentNotFoundException("피드 댓글을 삭제하는 과정에서 존재하지 않은 댓글 id를 받았습니다."));

        if (comment.getUser().equals(user) && comment.getFeed().equals(feed)) {
            commentRepository.delete(comment);
        } else {
            throw new ResourceNotPermitException(FEED_NOT_FOUND, "피드 댓글을 삭제하는 과정에서 존재하지 않는 피드에 접근했거나 인가받지 못한 사용자로부터의 요청입니다.");
        }
    }

    public FeedCommentSliceResponse getSliceFeedComments(Long feedId, Pageable pageable) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new FeedNotFoundException("피드 댓글을 조회하면서 존재하지 않는 피드 id를 받았습니다."));

        List<Comment> comments = commentRepositoryCustom.findFeedCommentsByFeed(feed, pageable);

        List<FeedCommentResponse> responseComments = comments.stream()
                .map(comment -> FeedCommentResponse.builder().comment(comment).build()).toList();

        FeedCommentSliceResponse response = new FeedCommentSliceResponse();
        response.setComments(responseComments);
        response.setSize(comments.size());
        response.setHasNextSlice(hasNextFeedCommentSlice(feedId, pageable));

        return response;
    }

    private Boolean hasNextFeedCommentSlice(Long feedId, Pageable pageable) {
        String jpql = "select count(cnt) from (select c.id AS cnt from Comment c where c.feed.id = :feedId)";
        Long countQuery = em.createQuery(jpql, Long.class).setParameter("feedId", feedId).getSingleResult();
        return countQuery > ((pageable.getPageNumber() + 1L) * pageable.getPageSize());
    }

    @Transactional
    public Long likeFeed(String authorizationHeader, Long feedId) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "피드의 좋아요를 반영하면서 유효하지 않은 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("피드의 좋아요를 반영하면서 존재하지 않는 유저 id를 받았습니다."));
        Feed feed = feedRepository.findFeedJoinUserById(feedId).orElseThrow(() -> new FeedNotFoundException("피드의 좋아요를 반영하면서 존재하지 않는 피드 id를 받았습니다."));

        if (likeRepository.existsLikeByFeedAndUser(feed, user)) {
            throw new DuplicateLikeException("이미 좋아요를 누른 사용자가 같은 피드에 요청을 보냈습니다.");
        }
        likeRepository.save(Likes.builder().user(user).feed(feed).build());
        if (isNotFeedOfUser(user, feed)) {
            alertCreateService.saveFeedLikeAlert(user, feed, feed.getLikes().size());
        }
        return userId;
    }

    private static boolean isNotFeedOfUser(User user, Feed feed) {
        return !feed.getUser().equals(user);
    }

    @Transactional
    public Long undoLikeFeed(String authorizationHeader, Long feedId) {
        Long userId = tokenProvider.getUserId(authorizationHeader, "피드의 좋아요를 취소하는 과정에서 훼손된 토큰을 받았습니다.");
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("피드의 좋아요를 취소하는 과정에서 존재하지 않는 유저 id를 받았습니다."));
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new FeedNotFoundException("피드의 좋아요를 취소하는 과정에서 존재하지 않는 피드 id를 받았습니다."));
        Likes like = likeRepository.findLikesByFeedAndUser(feed, user).orElseThrow(() -> new CommentNotFoundException("피드의 좋아요를 취소하는 과정에서 존재하지 않는 좋아요 id를 받았습니다."));

        if (like.getUser().equals(user) && like.getFeed().equals(feed)) {
            likeRepository.delete(like);
            return userId;
        } else {
            throw new ResourceNotPermitException(COMMENT_NOT_FOUND, "피드의 좋아요를 삭제하는 과정에서 존재하지 않는 댓글에 접근했거나 인가받지 못한 사용자로부터의 요청입니다.");
        }
    }

    public int getFeedLikes(Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new FeedNotFoundException("피드 좋아요 개수를 조회하는 과정에서 존재하지 않는 피드 id를 요청했습니다."));
        return feed.getLikes().size();
    }
}
