package com.qiange.ragdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 检索调试请求体。
 */
@Data
public class RetrievalDebugRequest {

    /**
     * 要进行检索测试的真实问题
     */
    @NotBlank(message = "question 不能为空")
    private String question;

    /**
     * 可选的知识库分类过滤条件
     */
    private String category;
}
