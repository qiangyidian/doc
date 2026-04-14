package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封装在大模型生成结果中命中的参考（引用）知识片段。
 * 也是向客户端返回 RAG 处理过程中使用上下文的依据（溯源数据）。
 *
 * 为什么问答结果里一定要返回引用片段？
 * 这正是 RAG 的核心优势和与“模型自由发挥”（即所谓的“幻觉”）最大的区别之一。
 * 通过附加参考片段，你可以让前端或者调用方清楚地看到模型的回答到底基于哪些上下文字段。
 * 这对于提升系统回答的可信度和容错性（让用户知道哪里说错了）是非常重要的。
 */
@Data // 自动生成获取/设置字段的方法、equals、hashCode 等
@Builder // 提供链式构建的 Builder 方法用于赋值和初始化，非常直观且易于维护
@NoArgsConstructor // 反序列化（如 Jackson 框架）使用到的无参构造器
@AllArgsConstructor // 包含全部字段的构造器，供 Builder 使用
public class ReferenceChunkResponse {

    // 命中的原文片段所属的原始文件名称。通常会带在引用源中进行展示，如“来源文件：01-rag-intro.md”
    private String sourceFileName;

    // 命中的原文片段的来源绝对路径。可以在服务端或者更深层用于打开或者检索原始文档
    private String sourcePath;

    // 命中的原文片段所对应其所处文件分块操作的索引。在调试分块策略或者上下文拼合时很有用
    private Integer chunkIndex;

    // 命中的这段具体的参考资料文本内容。这就是真正会被送入 Prompt 供模型阅读并依赖的内容
    private String content;
}
