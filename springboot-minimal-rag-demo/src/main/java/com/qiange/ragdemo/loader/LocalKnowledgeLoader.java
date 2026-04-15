package com.qiange.ragdemo.loader;

import com.qiange.ragdemo.common.RagConstants;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地知识文件加载器。
 *
 * 这里我们先做最小能力：
 * - 支持 md/txt
 * - 把文件内容读取成一个 Spring AI Document
 * - 给 Document 附带最基础的 Metadata
 */
@Component
public class LocalKnowledgeLoader {

    /**
     * 将本地文件加载并转换为单个大的文档对象。
     *
     * @param filePath 本地文件的绝对或相对路径
     * @param category 文件的分类标签
     * @param title    文件的显示标题，如果为空则默认使用文件名
     * @return 包含文件内容和元数据的完整的 Document 对象
     */
    public Document load(String filePath, String category, String title) {
        try {
            Path path = Path.of(filePath);

            // 1. 检查文件是否存在
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("文件不存在：" + filePath);
            }

            // 2. 获取文件名及扩展名
            String fileName = path.getFileName().toString();
            String suffix = getSuffix(fileName);

            // 3. 校验支持的文件类型
            if (!"md".equalsIgnoreCase(suffix) && !"txt".equalsIgnoreCase(suffix)) {
                throw new IllegalArgumentException("当前最小版只支持 md/txt 文件，实际类型：" + suffix);
            }

            // 4. 读取文件全部文本内容（使用 UTF-8 编码）
            String content = Files.readString(path, StandardCharsets.UTF_8);

            // 5. 校验内容是否为空
            if (!StringUtils.hasText(content)) {
                throw new IllegalArgumentException("文件内容为空，不允许导入空文档");
            }

            // 6. 初始化基础的元数据 (Metadata)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(RagConstants.METADATA_SOURCE_FILE_NAME, fileName);
            metadata.put(RagConstants.METADATA_SOURCE_PATH, path.toAbsolutePath().toString());
            metadata.put(RagConstants.METADATA_CATEGORY, category);
            metadata.put(RagConstants.METADATA_TITLE, StringUtils.hasText(title) ? title : fileName);
            // 额外附加一个加载时间的时间戳信息
            metadata.put("loadedAt", LocalDateTime.now().toString());

            // 7. 构造并返回 Spring AI 框架的 Document 对象
            return new Document(content, metadata);
        } catch (IOException e) {
            throw new RuntimeException("读取知识文件失败：" + filePath, e);
        }
    }

    /**
     * 辅助方法：从文件名中提取后缀扩展名。
     */
    private String getSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1);
    }
}
