package com.example.esarticlesearch.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleView {

    private String id;
    private String title;
    private String summary;
    private String content;
    private String category;
    private String author;
    private List<String> tags;
    private String status;
    private LocalDateTime publishTime;
    private LocalDateTime updateTime;
    private Integer viewCount;
}