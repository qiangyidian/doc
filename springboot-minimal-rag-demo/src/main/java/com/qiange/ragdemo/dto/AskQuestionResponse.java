package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 返回给客户端的大模型 RAG（检索增强生成）问答结果对象。
 *
 * 在工业级应用中，一个完整的 RAG 问答响应最起码应该包含两类核心信息：
 * 1. answer：大语言模型基于提示词最终生成并返回的自然语言答案。
 * 2. references：本次生成该答案时，模型在底层参考了知识库里的哪些具体切块（溯源机制，提高可信度）。
 */
@Data // 自动生成字段访问方法
@Builder // 提供强大的构建者模式（Builder Pattern），方便链式调用实例化和赋值
@NoArgsConstructor // 需要无参构造用于如 Jackson 等反序列化操作
@AllArgsConstructor // Builder 模式底层所需的带参构造器
public class AskQuestionResponse {

    // 存储大模型生成的文本答案
    private String answer;

    // 列表类型，记录了检索召回并用于构建 Prompt 的具体知识切片详情
    private List<ReferenceChunkResponse> references;
}
