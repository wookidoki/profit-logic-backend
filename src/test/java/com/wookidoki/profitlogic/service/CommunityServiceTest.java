package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.common.exception.ResourceNotFoundException;
import com.wookidoki.profitlogic.common.exception.UnauthorizedAccessException;
import com.wookidoki.profitlogic.domain.BoardPost;
import com.wookidoki.profitlogic.domain.Comment;
import com.wookidoki.profitlogic.domain.User;
import com.wookidoki.profitlogic.dto.*;
import com.wookidoki.profitlogic.repository.BoardPostRepository;
import com.wookidoki.profitlogic.repository.CommentRepository;
import com.wookidoki.profitlogic.repository.ProjectRepository;
import com.wookidoki.profitlogic.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityService 단위 테스트")
class CommunityServiceTest {

    @Mock private BoardPostRepository boardPostRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private CommunityService communityService;

    private User createUser(Long id, String nickname) {
        return User.builder().id(id).email(id + "@test.com").password("encoded").nickname(nickname).build();
    }

    private BoardPost createPost(Long id, User user) {
        return BoardPost.builder().id(id).user(user).title("테스트 게시글").content("내용").build();
    }

    @Nested
    @DisplayName("게시글 CRUD")
    class PostCrud {

        @Test
        @DisplayName("게시글 생성 성공")
        void createPost_success() {
            User user = createUser(1L, "작성자");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(boardPostRepository.save(any(BoardPost.class))).willAnswer(inv -> {
                BoardPost p = inv.getArgument(0);
                return BoardPost.builder().id(100L).user(p.getUser())
                        .title(p.getTitle()).content(p.getContent()).build();
            });

            BoardPostCreateRequest request = BoardPostCreateRequest.builder()
                    .title("새 게시글").content("내용").build();

            BoardPostResponse result = communityService.createPost(1L, request);

            assertThat(result.getTitle()).isEqualTo("새 게시글");
            assertThat(result.getCommentCount()).isZero();
        }

        @Test
        @DisplayName("게시글 목록 조회 — countByPostId 사용 (N+1 해결)")
        void getPosts_usesCountQuery() {
            User user = createUser(1L, "작성자");
            BoardPost post = createPost(10L, user);
            Page<BoardPost> page = new PageImpl<>(List.of(post));
            Pageable pageable = PageRequest.of(0, 10);

            given(boardPostRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(page);
            given(commentRepository.countByPostId(10L)).willReturn(5L);

            Page<BoardPostResponse> result = communityService.getPosts(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCommentCount()).isEqualTo(5);
            verify(commentRepository).countByPostId(10L);
            verify(commentRepository, never()).findByPostIdOrderByCreatedAtAsc(any());
        }

        @Test
        @DisplayName("게시글 상세 조회 — 조회수 증가 + countByPostId 사용")
        void getPost_incrementsViewAndUsesCount() {
            User user = createUser(1L, "작성자");
            BoardPost post = createPost(10L, user);
            given(boardPostRepository.findById(10L)).willReturn(Optional.of(post));
            given(commentRepository.countByPostId(10L)).willReturn(3L);

            BoardPostResponse result = communityService.getPost(10L);

            assertThat(result.getCommentCount()).isEqualTo(3);
            assertThat(post.getViewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("게시글 삭제 — deleteByPostId로 댓글 일괄 삭제")
        void deletePost_bulkDeletesComments() {
            User user = createUser(1L, "작성자");
            BoardPost post = createPost(10L, user);
            given(boardPostRepository.findById(10L)).willReturn(Optional.of(post));

            communityService.deletePost(10L, 1L);

            verify(commentRepository).deleteByPostId(10L);
            verify(boardPostRepository).delete(post);
        }

        @Test
        @DisplayName("비작성자가 게시글 삭제 시도 → UnauthorizedAccessException")
        void deletePost_notAuthor_throws() {
            User user = createUser(1L, "작성자");
            BoardPost post = createPost(10L, user);
            given(boardPostRepository.findById(10L)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> communityService.deletePost(10L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verify(boardPostRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 게시글 조회 → ResourceNotFoundException")
        void getPost_notFound_throws() {
            given(boardPostRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> communityService.getPost(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("댓글 CRUD")
    class CommentCrud {

        @Test
        @DisplayName("댓글 생성 성공")
        void createComment_success() {
            User user = createUser(1L, "댓글러");
            BoardPost post = createPost(10L, createUser(2L, "작성자"));
            given(boardPostRepository.findById(10L)).willReturn(Optional.of(post));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(commentRepository.save(any(Comment.class))).willAnswer(inv -> {
                Comment c = inv.getArgument(0);
                return Comment.builder().id(100L).post(c.getPost()).user(c.getUser()).content(c.getContent()).build();
            });

            CommentCreateRequest request = CommentCreateRequest.builder().content("좋은 글입니다").build();

            CommentResponse result = communityService.createComment(10L, 1L, request);

            assertThat(result.getContent()).isEqualTo("좋은 글입니다");
        }

        @Test
        @DisplayName("댓글 목록 조회 성공")
        void getComments_success() {
            User user = createUser(1L, "댓글러");
            BoardPost post = createPost(10L, user);
            Comment comment = Comment.builder().id(1L).post(post).user(user).content("댓글").build();

            given(boardPostRepository.findById(10L)).willReturn(Optional.of(post));
            given(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).willReturn(List.of(comment));

            List<CommentResponse> result = communityService.getComments(10L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("비작성자가 댓글 삭제 시도 → UnauthorizedAccessException")
        void deleteComment_notAuthor_throws() {
            User user = createUser(1L, "작성자");
            Comment comment = Comment.builder().id(100L).user(user).content("댓글").build();
            given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

            assertThatThrownBy(() -> communityService.deleteComment(100L, 999L))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }
}
