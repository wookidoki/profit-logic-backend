package com.wookidoki.profitlogic.dto.community;

import com.wookidoki.profitlogic.domain.BoardPost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardPostResponse {

    private Long id;
    private String title;
    private String content;
    private String authorNickname;
    private Long authorId;
    private Long projectId;
    private Integer viewCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BoardPostResponse from(BoardPost post, int commentCount) {
        return BoardPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorNickname(post.getUser().getNickname())
                .authorId(post.getUser().getId())
                .projectId(post.getProject() != null ? post.getProject().getId() : null)
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
