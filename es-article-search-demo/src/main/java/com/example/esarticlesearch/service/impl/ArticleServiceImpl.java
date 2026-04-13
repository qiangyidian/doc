package com.example.esarticlesearch.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.esarticlesearch.document.ArticleDocument;
import com.example.esarticlesearch.dto.ArticleCreateRequest;
import com.example.esarticlesearch.dto.ArticleSearchRequest;
import com.example.esarticlesearch.dto.ArticleSearchResponse;
import com.example.esarticlesearch.dto.ArticleView;
import com.example.esarticlesearch.repository.ArticleRepository;
import com.example.esarticlesearch.service.ArticleService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("publishTime", "updateTime", "viewCount");

    private final ArticleRepository articleRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void resetAndLoadDemoData() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ArticleDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping(ArticleDocument.class));

        List<ArticleDocument> articles = List.of(
            ArticleDocument.builder()
                .id(UUID.randomUUID().toString())
                .title("Spring Boot Elasticsearch Guide")
                .summary("A beginner guide for integrating Spring Boot with Elasticsearch")
                .content("This article introduces index creation, document mapping, query building and highlight usage.")
                .category("backend")
                .author("Alice")
                .tags(List.of("spring", "elasticsearch", "search"))
                .status("PUBLISHED")
                .publishTime(LocalDateTime.now().minusDays(6))
                .updateTime(LocalDateTime.now().minusDays(2))
                .viewCount(1200)
                .build(),
            ArticleDocument.builder()
                .id(UUID.randomUUID().toString())
                .title("How to Design Search Features")
                .summary("Understand the difference between full text search and exact filtering")
                .content("A good search feature starts from field modeling, then query DSL, and then result rendering.")
                .category("architecture")
                .author("Bob")
                .tags(List.of("search", "architecture"))
                .status("PUBLISHED")
                .publishTime(LocalDateTime.now().minusDays(4))
                .updateTime(LocalDateTime.now().minusDays(1))
                .viewCount(860)
                .build(),
            ArticleDocument.builder()
                .id(UUID.randomUUID().toString())
                .title("Content Review Draft")
                .summary("This is an internal draft article")
                .content("The article is still under editing workflow and should not be exposed to end users.")
                .category("process")
                .author("Alice")
                .tags(List.of("workflow", "draft"))
                .status("DRAFT")
                .publishTime(LocalDateTime.now().minusDays(1))
                .updateTime(LocalDateTime.now())
                .viewCount(16)
                .build()
        );

        articleRepository.saveAll(articles);
    }

    @Override
    public ArticleDocument save(ArticleCreateRequest request) {
        ArticleDocument document = ArticleDocument.builder()
            .id(UUID.randomUUID().toString())
            .title(request.getTitle())
            .summary(request.getSummary())
            .content(request.getContent())
            .category(request.getCategory())
            .author(request.getAuthor())
            .tags(request.getTags())
            .status(request.getStatus())
            .publishTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .viewCount(0)
            .build();

        return articleRepository.save(document);
    }

    @Override
    public ArticleSearchResponse search(ArticleSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolean hasCondition = false;

        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(must -> must.multiMatch(multi -> multi
                .query(request.getKeyword())
                .fields("title", "summary", "content")
            ));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getCategory())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("category").value(request.getCategory())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getTag())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("tags").value(request.getTag())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getAuthor())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("author").value(request.getAuthor())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getStatus())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("status").value(request.getStatus())));
            hasCondition = true;
        }

        String sortField = ALLOWED_SORT_FIELDS.contains(request.getSortField()) ? request.getSortField() : "publishTime";
        SortOrder sortOrder = "asc".equalsIgnoreCase(request.getSortDirection()) ? SortOrder.Asc : SortOrder.Desc;
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        Query query = hasCondition
            ? boolBuilder.build()._toQuery()
            : Query.of(q -> q.matchAll(m -> m));

        Highlight highlight = new Highlight(
            HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build(),
            List.of(
                new HighlightField("title"),
                new HighlightField("content")
            )
        );

        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(query)
            .withPageable(PageRequest.of(pageNum - 1, pageSize))
            .withSort(sort -> sort.field(field -> field.field(sortField).order(sortOrder)))
            .withHighlightQuery(new HighlightQuery(highlight, ArticleDocument.class))
            .build();

        SearchHits<ArticleDocument> hits = elasticsearchOperations.search(nativeQuery, ArticleDocument.class);
        List<ArticleView> items = new ArrayList<>();

        for (SearchHit<ArticleDocument> hit : hits) {
            ArticleDocument article = hit.getContent();
            Map<String, List<String>> highlightFields = hit.getHighlightFields();

            String title = article.getTitle();
            if (!CollectionUtils.isEmpty(highlightFields.get("title"))) {
                title = highlightFields.get("title").get(0);
            }

            String content = article.getContent();
            if (!CollectionUtils.isEmpty(highlightFields.get("content"))) {
                content = highlightFields.get("content").get(0);
            }

            items.add(ArticleView.builder()
                .id(article.getId())
                .title(title)
                .summary(article.getSummary())
                .content(content)
                .category(article.getCategory())
                .author(article.getAuthor())
                .tags(article.getTags())
                .status(article.getStatus())
                .publishTime(article.getPublishTime())
                .updateTime(article.getUpdateTime())
                .viewCount(article.getViewCount())
                .build());
        }

        return ArticleSearchResponse.builder()
            .total(hits.getTotalHits())
            .pageNum(pageNum)
            .pageSize(pageSize)
            .items(items)
            .build();
    }

    @Override
    public ArticleDocument getById(String id) {
        return articleRepository.findById(id).orElse(null);
    }
}
