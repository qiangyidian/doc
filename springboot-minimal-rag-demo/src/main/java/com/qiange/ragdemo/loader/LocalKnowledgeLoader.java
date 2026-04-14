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
 * - 支持 md
 * - 支持 txt
 * - 把文件内容读取成一个 Spring AI Document
 * - 给 Document 附带最基础的 Metadata
 *
 * 为什么这里不一开始就上 PDF / Word / Excel？
 * 因为教程强调过，文档处理是一个独立工程。
 * 最小项目先把纯文本链路打通，再逐步扩展格式支持，学习曲线会更稳。
 */
@Component
public class LocalKnowledgeLoader {

    /**
     * 根据给定的文件路径、类别和标题，将本地文件读取并转换为大模型可识别的 Document 对象。
     *
     * @param filePath 待读取的本地文件绝对或相对路径
     * @param category 文件的所属分类（例如：法律法规、操作手册等），用于后续检索时过滤
     * @param title    文件的显示标题，如果未提供，则默认使用文件名
     * @return 包含文件纯文本内容及相关元数据的 Spring AI Document 实例
     * @throws IllegalArgumentException 如果文件不存在、格式不支持或内容为空
     * @throws RuntimeException 如果在读取文件 IO 过程中发生异常
     */
    public Document load(String filePath, String category, String title) {
        try {
            // 将字符串形式的路径转换为 NIO 的 Path 对象
            Path path = Path.of(filePath);

            // 检查目标文件是否存在于文件系统中
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("文件不存在：" + filePath);
            }

            // 获取文件名，例如： "example.txt"
            String fileName = path.getFileName().toString();
            // 提取文件后缀，以判断文件类型
            String suffix = getSuffix(fileName);

            // 当前最小实现版本仅支持 .md 和 .txt 格式的纯文本文件
            if (!"md".equalsIgnoreCase(suffix) && !"txt".equalsIgnoreCase(suffix)) {
                throw new IllegalArgumentException("当前最小版只支持 md/txt 文件，实际类型：" + suffix);
            }

            // 以 UTF-8 编码读取整个文件的内容到字符串中
            String content = Files.readString(path, StandardCharsets.UTF_8);

            // 检查文件内容是否为空，避免导入无效的数据
            if (!StringUtils.hasText(content)) {
                throw new IllegalArgumentException("文件内容为空，不允许导入空文档");
            }

            // 初始化元数据 Map。元数据非常关键，除了内容，大模型或向量库需要知道这块文本“是谁的、从哪来的”
            Map<String, Object> metadata = new HashMap<>();
            // 记录原始文件名
            metadata.put(RagConstants.METADATA_SOURCE_FILE_NAME, fileName);
            // 记录原始文件的绝对路径
            metadata.put(RagConstants.METADATA_SOURCE_PATH, path.toAbsolutePath().toString());
            // 记录用户指定的分类，方便 RAG 检索时按分类过滤（FilterExpression）
            metadata.put(RagConstants.METADATA_CATEGORY, category);
            // 记录标题，若用户没传，则用文件名代替
            metadata.put(RagConstants.METADATA_TITLE, StringUtils.hasText(title) ? title : fileName);
            // 记录文档加载进系统的时间戳
            metadata.put("loadedAt", LocalDateTime.now().toString());

            // 将纯文本内容和对应的元数据封装成一个 Spring AI 的 Document 对象并返回
            return new Document(content, metadata);
        } catch (IOException e) {
            // 将受检异常转换为运行时异常并抛出，中止当前加载流程
            throw new RuntimeException("读取知识文件失败：" + filePath, e);
        }
    }

    /**
     * 辅助方法：从文件名中提取文件后缀（不带点号）。
     * 
     * @param fileName 完整文件名
     * @return 文件的后缀名。如果没有后缀或以点结尾，则返回空字符串。
     */
    private String getSuffix(String fileName) {
        // 找到文件名中最后一个 '.' 的索引位置
        int index = fileName.lastIndexOf('.');
        // 如果没有找到 '.'，或者 '.' 是文件名的最后一个字符，说明没有后缀
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        // 截取 '.' 之后的所有字符作为后缀
        return fileName.substring(index + 1);
    }
}
