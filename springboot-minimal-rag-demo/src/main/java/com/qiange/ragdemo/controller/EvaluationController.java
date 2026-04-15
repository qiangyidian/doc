package com.qiange.ragdemo.controller;

import com.qiange.ragdemo.common.Result;
import com.qiange.ragdemo.dto.RetrievalEvaluationResponse;
import com.qiange.ragdemo.service.RetrievalEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评估接口。
 * 提供触发检索策略评估的 HTTP 端点。
 */
@RestController // 声明为 REST 控制器，返回 JSON 响应
@RequestMapping("/api/evaluation") // 定义此控制器的基础请求路径
@RequiredArgsConstructor // Lombok 注解，自动生成包含 final 字段的构造函数
public class EvaluationController {

    // 检索评估服务，负责运行评估案例并统计命中率等指标
    private final RetrievalEvaluationService retrievalEvaluationService;

    /**
     * 执行检索策略评估。
     * 读取配置的评估样本文件，逐个测试当前检索参数下的命中情况并返回统计结果。
     *
     * @return 包含评估样本总数、Hit Rate、MRR 以及每条样本明细的响应体
     */
    @PostMapping("/retrieval/run") // 映射 HTTP POST 请求到 /api/evaluation/retrieval/run
    public Result<RetrievalEvaluationResponse> runRetrievalEvaluation() {
        // 调用服务执行评估
        RetrievalEvaluationResponse response = retrievalEvaluationService.evaluate();
        // 返回评估结果
        return Result.ok(response);
    }
}
