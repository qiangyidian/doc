package com.example.esarticlesearch.service;

import com.example.esarticlesearch.document.ArticleDocument;
import com.example.esarticlesearch.dto.ArticleCreateRequest;
import com.example.esarticlesearch.dto.ArticleSearchRequest;
import com.example.esarticlesearch.dto.ArticleSearchResponse;

public interface ArticleService {

    void resetAndLoadDemoData();

    ArticleDocument save(ArticleCreateRequest request);

    ArticleSearchResponse search(ArticleSearchRequest request);

    ArticleDocument getById(String id);
}