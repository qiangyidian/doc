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
 */
@Component
@RequiredArgsConstructor
public class KnowledgeChunker {

    // RAG 相关配置属性
    private final RagProperties ragProperties;

    /**
     * 将长文档切分为多个重叠的文档块。
     *
     * @param originalDocument 原始文档对象
     * @return 切分后的文档块列表
     */
    public List<Document> split(Document originalDocument) {
        String originalText = originalDocument.getText();
        // 归一化换行符并去除首尾空白
        String normalizedText = originalText.replace("\r\n", "\n").trim();

        // 从配置中获取切分参数
        Integer chunkSize = ragProperties.getKnowledgeBase().getChunkSize();
        Integer chunkOverlap = ragProperties.getKnowledgeBase().getChunkOverlap();

        // 计算步长
        int step = chunkSize - chunkOverlap;
        if (step <= 0) {
            throw new IllegalArgumentException("chunk-size 必须大于 chunk-overlap，否则会导致死循环");
        }

        List<Document> result = new ArrayList<>();
        int start = 0;
        int chunkIndex = 0;

        // 滑动窗口切分循环
        while (start < normalizedText.length()) {
            // 计算当前块的结束位置
            int end = Math.min(start + chunkSize, normalizedText.length());
            // 截取当前块的内容并去除首尾空白
            String chunkText = normalizedText.substring(start, end).trim();

            if (!chunkText.isEmpty()) {
                // 复制原始文档的元数据
                Map<String, Object> metadata = new HashMap<>(originalDocument.getMetadata());
                // 添加切块特定的元数据
                metadata.put(RagConstants.METADATA_CHUNK_INDEX, chunkIndex);
                metadata.put(RagConstants.METADATA_START_OFFSET, start);
                metadata.put(RagConstants.METADATA_END_OFFSET, end);

                // 创建并添加切分后的文档对象
                result.add(new Document(chunkText, metadata));
                chunkIndex++;
            }

            // 如果已经到达文本末尾，结束循环
            if (end >= normalizedText.length()) {
                break;
            }

            // 移动窗口起点
            start += step;
        }

        return result;
    }
}
