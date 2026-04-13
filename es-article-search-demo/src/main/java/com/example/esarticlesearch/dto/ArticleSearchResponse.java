package com.example.esarticlesearch.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleSearchResponse {

    private long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<ArticleView> items;
}