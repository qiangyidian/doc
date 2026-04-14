package com.qiange.ragdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 接收客户端 RAG（检索增强生成）问答请求的 DTO 对象。
 * 用于封装用户的提问和可选的检索过滤条件。
 */
@Data // 自动生成字段的 Getter、Setter、toString 等方法
public class AskQuestionRequest {

    /**
     * 用户输入的真实问题。
     * 使用 @NotBlank 约束，确保调用接口时这个字段不能为 null 且不能是空白字符串。
     * 如果违反约束，将在 Controller 层抛出参数校验异常。
     */
    @NotBlank(message = "question 不能为空")
    private String question;

    /**
     * 可选的知识库分类过滤条件（Category Filter）。
     * 如果客户端传递了该字段，系统在执行相似度检索时，将仅在对应分类的知识片段中搜索；
     * 如果不传或为空，则在全局知识库中进行全量检索。
     */
    private String category;
}
