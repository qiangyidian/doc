package com.example.esarticlesearch.repository;

import com.example.esarticlesearch.document.ArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String> {
}