package com.example.esarticlesearch.controller;

import com.example.esarticlesearch.common.Result;
import com.example.esarticlesearch.document.ArticleDocument;
import com.example.esarticlesearch.dto.ArticleCreateRequest;
import com.example.esarticlesearch.dto.ArticleSearchRequest;
import com.example.esarticlesearch.dto.ArticleSearchResponse;
import com.example.esarticlesearch.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping("/init-demo")
    public Result<String> initDemo() {
        articleService.resetAndLoadDemoData();
        return Result.success("文章索引已重建并写入演示数据");
    }

    @PostMapping
    public Result<ArticleDocument> create(@Valid @RequestBody ArticleCreateRequest request) {
        return Result.success(articleService.save(request));
    }

    @PostMapping("/search")
    public Result<ArticleSearchResponse> search(@RequestBody ArticleSearchRequest request) {
        return Result.success(articleService.search(request));
    }

    @GetMapping("/{id}")
    public Result<ArticleDocument> getById(@PathVariable String id) {
        return Result.success(articleService.getById(id));
    }
}