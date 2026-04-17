package com.qiange.ragdemo.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 压缩结构化输出包裹体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompressionResultEnvelope {

    private List<CompressionDecision> results;
}
