package com.connectsphere.post.service;

import com.connectsphere.post.client.SearchClient;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SearchClient searchClient;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    void createPostDefaultsVisibilityAndTypeThenIndexesSavedPost() {
        Post input = new Post();
        input.setAuthorId(7);
        input.setContent("hello world");

        Post saved = new Post();
        saved.setPostId(11);
        saved.setAuthorId(7);
        saved.setContent("hello world");
        saved.setVisibility("PUBLIC");
        saved.setPostType("TEXT");

        when(postRepository.save(input)).thenReturn(saved);

        Post result = postService.createPost(input);

        assertThat(result).isSameAs(saved);
        assertThat(input.getVisibility()).isEqualTo("PUBLIC");
        assertThat(input.getPostType()).isEqualTo("TEXT");
        assertThat(input.isDeleted()).isFalse();
        verify(searchClient).indexPost(11, "hello world", 7, "PUBLIC", "TEXT");
    }

    @Test
    void getFeedForUserReturnsRepositoryFeedForFollowees() {
        ReflectionTestUtils.setField(postService, "followServiceUrl", "http://follow-service");
        List<Integer> followees = List.of(2, 3);
        Post first = new Post();
        first.setPostId(101);

        when(restTemplate.exchange(
                eq("http://follow-service/follows/1/following-ids"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(followees));
        when(postRepository.findFeedByUserIds(followees)).thenReturn(List.of(first));

        List<Post> result = postService.getFeedForUser(1);

        assertThat(result).containsExactly(first);
        verify(postRepository).findFeedByUserIds(followees);
    }

    @Test
    void decrementLikesNeverGoesBelowZero() {
        Post post = new Post();
        post.setPostId(9);
        post.setLikesCount(0);

        when(postRepository.findByPostIdAndIsDeletedFalse(9)).thenReturn(Optional.of(post));

        postService.decrementLikes(9);

        assertThat(post.getLikesCount()).isZero();
        verify(postRepository).save(post);
    }

    @Test
    void changeVisibilityRejectsInvalidVisibilityWithoutSaving() {
        assertThatThrownBy(() -> postService.changeVisibility(1, "FRIENDS"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid visibility");

        verify(postRepository, never()).save(any());
        verify(searchClient, never()).indexPost(any(Integer.class), any(), any(Integer.class), any(), any());
    }
}
