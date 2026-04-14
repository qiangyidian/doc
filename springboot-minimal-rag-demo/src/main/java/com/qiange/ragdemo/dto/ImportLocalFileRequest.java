package com.qiange.ragdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 将本地知识文件导入到系统的请求体 DTO。
 *
 * 这种方案是最小化版本中最实用的一种导入方式：
 * 直接传递一个服务器上存在的本地绝对路径，将指定的 md/txt 文件解析并导入向量库。
 */
@Data // 自动生成字段访问方法及 toString() 等
public class ImportLocalFileRequest {

    /**
     * 待导入的本地文件系统中的绝对路径。
     * 例如（Windows）：C:/Gitee/RAG/knowledge-base/01-rag-intro.md
     * 或者（Linux/Mac）：/var/lib/knowledge-base/01-rag-intro.md
     * 不能为空，通过 @NotBlank 约束进行入参校验。
     */
    @NotBlank(message = "filePath 不能为空")
    private String filePath;

    /**
     * 知识归属的分类标签。
     * 这是一个可选字段。如果请求中不传或传空，后端的业务逻辑会自动降级，
     * 使用 yml 配置文件中定义的 `rag.knowledge-base.default-category`。
     */
    private String category;

    /**
     * 用户自定义的知识展示标题。
     * 这是一个可选字段。如果提供了，会被存储在切片（Chunk）的元数据中，
     * 用于前端更好地展示知识片段来源；如果不提供，通常会直接使用文件名称来代替。
     * 主要用于演示 Metadata 在 RAG 中的灵活扩展能力。
     */
    private String title;
}
