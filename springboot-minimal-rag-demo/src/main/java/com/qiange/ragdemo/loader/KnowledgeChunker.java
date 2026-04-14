package com.qiange.ragdemo.loader;

import com.qiange.ragdemo.common.RagConstants;
import com.qiange.ragdemo.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本分块器。
 *
 * 这里我们没有直接使用黑盒分块器，而是手写一个“字符窗口 + overlap”的最小实现。
 * 原因很简单：
 * 1. 教学更透明，你能直观看懂 chunk-size 和 overlap 是怎么生效的
 * 2. 更容易和教程中的分块概念一一对应
 * 3. 后面你想替换成 TokenTextSplitter、递归分块器也更容易
 */
@Component
@RequiredArgsConstructor
public class KnowledgeChunker {

    // 引入RAG配置属性，用于获取分块大小和重叠大小
    private final RagProperties ragProperties;

    /**
     * 将单个文档拆分为多个带有重叠的文本块（Document）列表。
     *
     * @param originalDocument 原始文档对象
     * @return 分块后的文档列表
     */
    public List<Document> split(Document originalDocument) {
        // 获取原始文档的文本内容
        String originalText = originalDocument.getText();
        // 标准化文本：将换行符 \r\n 替换为 \n，并去除首尾空白字符
        String normalizedText = originalText.replace("\r\n", "\n").trim();

        // 从配置中获取每个分块的最大字符数
        Integer chunkSize = ragProperties.getKnowledgeBase().getChunkSize();
        // 从配置中获取相邻分块之间的重叠字符数
        Integer chunkOverlap = ragProperties.getKnowledgeBase().getChunkOverlap();

        // 计算滑动窗口的步长（分块大小减去重叠大小）
        int step = chunkSize - chunkOverlap;
        // 校验步长，防止重叠部分过大导致无法向前推进（即死循环）
        if (step <= 0) {
            throw new IllegalArgumentException("chunk-size 必须大于 chunk-overlap，否则会导致死循环");
        }

        // 初始化用于存储最终分块结果的集合
        List<Document> result = new ArrayList<>();
        // 记录当前分块起始位置在字符串中的索引
        int start = 0;
        // 记录当前分块的序号
        int chunkIndex = 0;

        // 当起始位置还没有到达文本末尾时，循环分块
        while (start < normalizedText.length()) {
            // 计算当前分块的结束位置，不能超过文本总长度
            int end = Math.min(start + chunkSize, normalizedText.length());
            // 截取当前分块的文本，并去除首尾的空白字符
            String chunkText = normalizedText.substring(start, end).trim();

            // 如果分块文本不为空，则构建新的 Document 对象
            if (!chunkText.isEmpty()) {
                // 复制原始文档的元数据，以保留文档的来源信息等
                Map<String, Object> metadata = new HashMap<>(originalDocument.getMetadata());
                // 在元数据中加入当前分块的序号
                metadata.put(RagConstants.METADATA_CHUNK_INDEX, chunkIndex);
                // 在元数据中记录当前分块在原文本中的起始偏移量
                metadata.put(RagConstants.METADATA_START_OFFSET, start);
                // 在元数据中记录当前分块在原文本中的结束偏移量
                metadata.put(RagConstants.METADATA_END_OFFSET, end);

                // 使用截取的文本和补充后的元数据，创建一个新的 Document 并添加到结果列表中
                result.add(new Document(chunkText, metadata));
                // 分块序号递增
                chunkIndex++;
            }

            // 如果当前分块的结束位置已经达到或超过了文本的末尾，则说明整个文档已经处理完毕，跳出循环
            if (end >= normalizedText.length()) {
                break;
            }

            // 起始位置按照步长向前推进，为截取下一个分块做准备
            start += step;
        }

        // 返回分块后的所有文档块
        return result;
    }
}
