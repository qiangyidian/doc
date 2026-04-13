package com.example.esarticlesearch.dto;

import lombok.Data;

@Data
public class ArticleSearchRequest {

    private String keyword;
    private String category;
    private String tag;
    private String author;
    private String status;
    private String sortField = "publishTime";
    private String sortDirection = "desc";
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}