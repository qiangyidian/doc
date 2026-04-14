package com.qiange.ragdemo.service.impl;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.dto.AskQuestionRequest;
import com.qiange.ragdemo.dto.AskQuestionResponse;
import com.qiange.ragdemo.dto.ReferenceChunkResponse;
import com.qiange.ragdemo.service.RagChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 问答服务实现。
 *
 * 整条主链路如下：
 * 1. 用用户问题去向量库检索相关片段
 * 2. 把片段整理成上下文
 * 3. 把“问题 + 上下文”一起交给大模型
 * 4. 返回答案，并把命中的片段一起带回去
 */
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    // 向量存储库，用于根据问题进行相似度搜索（检索包含知识的 Document 列表）
    private final VectorStore vectorStore;

    // 大模型聊天客户端，用于与底层的大语言模型（如 OpenAI, 通义千问等）通信，生成最终答案
    private final ChatClient chatClient;

    // RAG 配置属性，包含检索参数（如 topK、相似度阈值等）的设定
    private final RagProperties ragProperties;

    /**
     * 根据用户的提问进行 RAG（检索增强生成）查询并返回答案。
     *
     * @param request 包含用户问题和（可选的）分类过滤条件
     * @return 包含大模型生成的答案和参考资料片段的响应对象
     */
    @Override
    public AskQuestionResponse ask(AskQuestionRequest request) {
        // 1. 构建向量检索请求，包含用户原始问题、想要检索的文档数量(topK)以及相似度阈值
        SearchRequest.Builder searchBuilder = SearchRequest.builder()
                .query(request.getQuestion())
                .topK(ragProperties.getRetrieval().getTopK())
                .similarityThreshold(ragProperties.getRetrieval().getSimilarityThreshold());

        // 如果用户在请求中指定了某个分类，则在检索时增加分类过滤条件
        if (StringUtils.hasText(request.getCategory())) {
            searchBuilder.filterExpression("category == '" + escapeFilterValue(request.getCategory()) + "'");
        }

        // 2. 执行向量检索，从向量库中寻找与用户问题最相关的几个知识文档（Document）片段
        List<Document> retrievedDocuments = vectorStore.similaritySearch(searchBuilder.build());

        // 3. 校验检索结果，如果什么也没搜到，说明知识库中缺乏相关信息，直接返回提示，不再调用大模型
        if (retrievedDocuments == null || retrievedDocuments.isEmpty()) {
            return AskQuestionResponse.builder()
                    .answer("当前知识库中没有检索到足够相关的内容，请先补充知识文档后再提问。")
                    .references(List.of())
                    .build();
        }

        // 4. 将检索到的 Document 列表格式化成一个巨大的纯文本字符串，以便放进 prompt 中给大模型阅读
        String context = buildContext(retrievedDocuments);

        // 5. 构建系统提示词（System Prompt），在这里给大模型立规矩，比如：必须基于知识回答、不能瞎编
        String systemPrompt = """
                你是一个严格基于知识库回答问题的助手。
                你的回答必须遵守以下规则：
                1. 优先依据提供的知识片段回答
                2. 如果知识片段不足以回答，就明确说明“根据当前知识库无法确定”
                3. 不要编造知识库中不存在的事实
                4. 回答尽量结构化、简洁、可执行
                """;

        // 6. 构建用户提示词（User Prompt），将“整理好的参考上下文”和“用户实际的问题”拼接在一起
        String userPrompt = """
                请基于以下知识片段回答问题。

                【知识片段开始】
                %s
                【知识片段结束】

                用户问题：
                %s
                """.formatted(context, request.getQuestion());

        // 7. 发起大模型调用（Generation）。带上系统指令和用户输入，同步等待模型生成最终答案内容
        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        // 8. 将刚才检索到的原始 Document 对象列表转换为面向前端的 DTO 对象（ReferenceChunkResponse），包含文档来源和片段内容等
        List<ReferenceChunkResponse> references = retrievedDocuments.stream()
                .map(this::toReferenceChunkResponse)
                .collect(Collectors.toList());

        // 9. 封装并返回最终结果：大模型的答案 + 检索到的相关知识片段
        return AskQuestionResponse.builder()
                .answer(answer)
                .references(references)
                .build();
    }

    /**
     * 将检索出的一组 Document 组装成一个用于 prompt 的大段上下文文本。
     */
    private String buildContext(List<Document> documents) {
        StringBuilder builder = new StringBuilder();

        // 遍历每个文档片段，提取文本及其来源元数据进行格式化拼接
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            Map<String, Object> metadata = document.getMetadata();

            builder.append("片段").append(i + 1).append("：").append("\n");
            // 加入文档来源文件名，方便模型（或者调试）知晓这段文本的来源
            builder.append("来源文件：")
                    .append(metadata.getOrDefault(RagConstants.METADATA_SOURCE_FILE_NAME, "unknown"))
                    .append("\n");
            // 加入文档分块序号
            builder.append("分块序号：")
                    .append(metadata.getOrDefault(RagConstants.METADATA_CHUNK_INDEX, -1))
                    .append("\n");
            // 加入文档的核心文本内容
            builder.append(document.getText()).append("\n\n");
        }

        return builder.toString();
    }

    /**
     * 将底层框架的 Document 对象映射转换为向客户端返回的 DTO 对象。
     */
    private ReferenceChunkResponse toReferenceChunkResponse(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return ReferenceChunkResponse.builder()
                .sourceFileName(stringValue(metadata.get(RagConstants.METADATA_SOURCE_FILE_NAME))) // 提取源文件名
                .sourcePath(stringValue(metadata.get(RagConstants.METADATA_SOURCE_PATH)))           // 提取源文件路径
                .chunkIndex(intValue(metadata.get(RagConstants.METADATA_CHUNK_INDEX)))              // 提取分块索引
                .content(document.getText())                                                         // 提取片段正文
                .build();
    }

    /**
     * 安全地将 Object 转换为 String，防 NullPointerException。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 安全地将 Object 转换为 Integer，防 NullPointerException。
     */
    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * 转义过滤表达式中的单引号，防止类似 SQL 注入的问题，确保 Spring AI 的 filterExpression 解析正确。
     */
    private String escapeFilterValue(String value) {
        return value.replace("'", "\\'");
    }
}
