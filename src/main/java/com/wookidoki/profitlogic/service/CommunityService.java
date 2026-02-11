package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.BoardPost;
import com.wookidoki.profitlogic.domain.Comment;
import com.wookidoki.profitlogic.domain.Project;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.*;
import com.wookidoki.profitlogic.repository.BoardPostRepository;
import com.wookidoki.profitlogic.repository.CommentRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final BoardPostRepository boardPostRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    // ── 게시글 ──

    @Transactional
    public BoardPostResponse createPost(Long userId, BoardPostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", userId));

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("프로젝트", request.getProjectId()));
        }

        BoardPost post = BoardPost.builder()
                .user(user)
                .project(project)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        BoardPost saved = boardPostRepository.save(post);
        return BoardPostResponse.from(saved, 0);
    }

    @Transactional(readOnly = true)
    public Page<BoardPostResponse> getPosts(Pageable pageable) {
        return boardPostRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(post -> {
                    int commentCount = commentRepository.countByPostId(post.getId());
                    return BoardPostResponse.from(post, commentCount);
                });
    }

    @Transactional
    public BoardPostResponse getPost(Long postId) {
        BoardPost post = findPostOrThrow(postId);
        post.incrementViewCount();
        int commentCount = commentRepository.countByPostId(postId);
        return BoardPostResponse.from(post, commentCount);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        BoardPost post = findPostOrThrow(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
        commentRepository.deleteAll(commentRepository.findByPostIdOrderByCreatedAtAsc(postId));
        boardPostRepository.delete(post);
    }

    // ── 댓글 ──

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentCreateRequest request) {
        BoardPost post = findPostOrThrow(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", userId));

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .build();

        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        findPostOrThrow(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글", commentId));
        if (!comment.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
        commentRepository.delete(comment);
    }

    private BoardPost findPostOrThrow(Long postId) {
        return boardPostRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글", postId));
    }
}
