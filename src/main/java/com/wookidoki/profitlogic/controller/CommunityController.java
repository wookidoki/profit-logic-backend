package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.community.BoardPostCreateRequest;
import com.wookidoki.profitlogic.dto.community.BoardPostResponse;
import com.wookidoki.profitlogic.dto.community.CommentCreateRequest;
import com.wookidoki.profitlogic.dto.community.CommentResponse;
import com.wookidoki.profitlogic.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // ── 게시글 ──

    @PostMapping("/posts")
    public ResponseEntity<ResponseData<BoardPostResponse>> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BoardPostCreateRequest request) {
        BoardPostResponse result = communityService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.success(result, "게시글이 작성되었습니다."));
    }

    @GetMapping("/posts")
    public ResponseEntity<ResponseData<Page<BoardPostResponse>>> getPosts(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoardPostResponse> result = communityService.getPosts(pageable);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ResponseData<BoardPostResponse>> getPost(@PathVariable Long postId) {
        BoardPostResponse result = communityService.getPost(postId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ResponseData<Void>> deletePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId) {
        communityService.deletePost(postId, userId);
        return ResponseEntity.ok(ResponseData.success(null, "게시글이 삭제되었습니다."));
    }

    // ── 댓글 ──

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ResponseData<CommentResponse>> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse result = communityService.createComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.success(result, "댓글이 작성되었습니다."));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ResponseData<List<CommentResponse>>> getComments(
            @PathVariable Long postId) {
        List<CommentResponse> result = communityService.getComments(postId);
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ResponseData<Void>> deleteComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId) {
        communityService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ResponseData.success(null, "댓글이 삭제되었습니다."));
    }
}
