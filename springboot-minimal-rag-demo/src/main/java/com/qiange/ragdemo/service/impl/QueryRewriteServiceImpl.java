package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.service.QueryRewriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 查询改写服务实现。
 *
 * 通过大模型对用户原始提问进行多视角或同义词扩展，生成多个查询变体，
 * 从而提高后续检索命中的概率和覆盖率。
 */
@Service
@RequiredArgsConstructor
public class QueryRewriteServiceImpl implements QueryRewriteService {

    // 大模型聊天客户端，负责发起提示词请求并解析回答
    private final ChatClient chatClient;

    // RAG 相关配置，包含是否开启查询重写以及重写的条数等参数
    private final RagProperties ragProperties;

    /**
     * 将用户输入的原问题改写成多个不同表达的检索词列表。
     *
     * @param question 用户输入的原始问题
     * @return 包含原始问题和重写后的多个变体的查询词集合
     */
    @Override
    public List<String> rewriteQueries(String question) {
        // 如果配置中关闭了查询重写，或者为 null，直接返回原始问题
        if (!Boolean.TRUE.equals(ragProperties.getRetrieval().getQueryRewriteEnabled())) {
            return List.of(question);
        }

        // 构建特定的提示词，约束模型生成的变体格式和数量
        String prompt = """
                你是一个 RAG 检索改写助手。
                请基于用户问题，生成 %s 条更适合知识检索的查询表达。

                约束：
                1. 不要改变原问题意图
                2. 尽量补全术语，而不是发散主题
                3. 每行返回一条，不要编号
                4. 不要输出解释

                用户问题：
                %s
                """.formatted(ragProperties.getRetrieval().getRewriteQueryCount(), question);

        // 使用 LinkedHashSet 保证生成的查询词有序，同时具备去重功能
        LinkedHashSet<String> querySet = new LinkedHashSet<>();
        // 首先把用户的原始提问也加进去，避免大模型把原意改写偏了导致丢失核心词
        querySet.add(question);

        try {
            // 调用大模型生成查询词变体
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 解析模型的返回结果，按行切割并去除空白字符，再加到集合里
            if (StringUtils.hasText(response)) {
                for (String line : response.split("\\R")) {
                    String trimmed = line.trim();
                    // 去掉如 "1. " 这样的潜在编号前缀（防呆）
                    if (StringUtils.hasText(trimmed)) {
                        querySet.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            // 如果调用大模型超时或者异常，进行降级处理，不抛出异常影响主流程，直接只返回原问题
            return List.of(question);
        }

        // 转为 List 返回
        return new ArrayList<>(querySet);
    }
}
