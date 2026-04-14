package com.qiange.ragdemo.service;

import com.qiange.ragdemo.dto.AskQuestionRequest;
import com.qiange.ragdemo.dto.AskQuestionResponse;

/**
 * RAG 问答服务接口定义。
 * 
 * 职责是接收用户的提问，然后结合知识库中的上下文，返回大模型生成的答案。
 */
public interface RagChatService {

    /**
     * 基于用户的请求和系统预设的检索配置进行回答。
     *
     * @param request 封装了用户提出的问题、分类过滤等条件
     * @return 包含最终生成的答案、以及被模型参考的原文片段信息的响应对象
     */
    AskQuestionResponse ask(AskQuestionRequest request);
}
