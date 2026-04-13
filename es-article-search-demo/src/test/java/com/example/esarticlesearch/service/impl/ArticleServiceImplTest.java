package com.example.esarticlesearch.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.esarticlesearch.document.ArticleDocument;
import com.example.esarticlesearch.dto.ArticleSearchRequest;
import com.example.esarticlesearch.dto.ArticleSearchResponse;
import com.example.esarticlesearch.repository.ArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Captor
    private ArgumentCaptor<NativeQuery> queryCaptor;

    @Test
    void searchShouldBuildHighlightQueryAndUseHighlightedFields() {
        ArticleDocument article = ArticleDocument.builder()
            .id("1")
            .title("plain title")
            .summary("plain summary")
            .content("plain content")
            .category("backend")
            .author("Alice")
            .tags(List.of("spring"))
            .status("PUBLISHED")
            .publishTime(LocalDateTime.of(2026, 4, 2, 10, 0))
            .updateTime(LocalDateTime.of(2026, 4, 2, 12, 0))
            .viewCount(12)
            .build();

        SearchHit<ArticleDocument> hit = new SearchHit<>(
            "article_index",
            "1",
            null,
            1.0f,
            null,
            Map.of(
                "title", List.of("<em>Spring</em> title"),
                "content", List.of("<em>Spring</em> content")
            ),
            Map.of(),
            null,
            null,
            List.of(),
            article
        );

        when(elasticsearchOperations.search(any(NativeQuery.class), any(Class.class)))
            .thenReturn(new SearchHitsImpl<>(1, TotalHitsRelation.EQUAL_TO, 1.0f, null, null, List.of(hit), null, null));

        ArticleSearchRequest request = new ArticleSearchRequest();
        request.setKeyword("Spring");

        ArticleSearchResponse response = articleService.search(request);

        verify(elasticsearchOperations).search(queryCaptor.capture(), any(Class.class));
        NativeQuery nativeQuery = queryCaptor.getValue();

        assertThat(nativeQuery.getHighlightQuery()).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTitle()).isEqualTo("<em>Spring</em> title");
        assertThat(response.getItems().get(0).getContent()).isEqualTo("<em>Spring</em> content");
    }
}
