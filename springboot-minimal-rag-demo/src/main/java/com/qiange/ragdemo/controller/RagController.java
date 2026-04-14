package com.qiange.ragdemo.controller;

import com.qiange.ragdemo.common.Result;
import com.qiange.ragdemo.dto.AskQuestionRequest;
import com.qiange.ragdemo.dto.AskQuestionResponse;
import com.qiange.ragdemo.service.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 问答接口。
 * 提供基于已导入知识库进行提问和获取回答的 HTTP 端点。
 */
@RestController // 声明这是一个 REST 控制器，方法返回的对象会自动序列化为 JSON
@RequestMapping("/api/rag") // 定义基础的 URL 映射路径
@RequiredArgsConstructor // 自动生成包含 final 成员变量的构造函数，用于依赖注入
public class RagController {

    // 注入 RAG 问答的核心业务服务
    private final RagChatService ragChatService;

    /**
     * 接收用户的提问，执行 RAG 检索和模型生成，并返回结果。
     *
     * @param request 封装了用户提问内容和可选过滤条件的请求对象，使用 @Valid 开启数据校验
     * @return 包含最终答案文本和参考引用片段的统一响应包装对象
     */
    @PostMapping("/ask") // 映射 HTTP POST 请求到 /api/rag/ask
    public Result<AskQuestionResponse> ask(@Valid @RequestBody AskQuestionRequest request) {
        // 调用业务层服务处理提问
        AskQuestionResponse response = ragChatService.ask(request);
        // 将结果包装在统一的 Result 对象中返回给客户端
        return Result.ok(response);
    }
}
