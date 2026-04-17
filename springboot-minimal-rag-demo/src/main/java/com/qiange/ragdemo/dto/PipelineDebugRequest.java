package com.qiange.ragdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三阶段流水线调试请求体。
 */
@Data
public class PipelineDebugRequest {

    @NotBlank(message = "question 不能为空")
    private String question;

    private String category;
}
