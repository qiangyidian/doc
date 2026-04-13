package com.example.esarticlesearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class ArticleCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "作者不能为空")
    private String author;

    @NotEmpty(message = "标签不能为空")
    private List<String> tags;

    @NotBlank(message = "状态不能为空")
    private String status;
}