package com.example.matchup.matchupbackend.dto.response.feed;

import com.example.matchup.matchupbackend.entity.Feed;
import com.example.matchup.matchupbackend.entity.ProjectDomain;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FeedSearchResponse {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String thumbnailUrl;
    private LocalDate createdDate;
    private String nickname;
    private String userPictureUrl;
    private Long positionLevel;
    private Boolean isLiked;
    private Long type;
    private ProjectDomain projectDomain;

    @Builder
    public FeedSearchResponse(Long id, Long userId, String title, String content, String thumbnailUrl, LocalDate createdDate, String nickname, String userPictureUrl, Long positionLevel, Boolean isLiked, Long type, ProjectDomain projectDomain) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        this.createdDate = createdDate;
        this.nickname = nickname;
        this.userPictureUrl = userPictureUrl;
        this.positionLevel = positionLevel;
        this.isLiked = isLiked;
        this.type = type;
        this.projectDomain = projectDomain;
    }

    public static FeedSearchResponse fromEntity(Feed feed){
        return FeedSearchResponse.builder()
                .id(feed.getId())
                .title(feed.getTitle())
                .content(feed.getContent())
                .thumbnailUrl(feed.getThumbnailUrl())
                .createdDate(feed.getCreateTime().toLocalDate())
                .nickname(feed.getUser().getNickname())
                .userPictureUrl(feed.getUser().getPictureUrl())
                .positionLevel(feed.getUser().getPositionLevel())
                .userId(feed.getUser().getId())
                .type(feed.getType())
                .projectDomain(feed.getProjectDomain())
                .build();
    }
}
