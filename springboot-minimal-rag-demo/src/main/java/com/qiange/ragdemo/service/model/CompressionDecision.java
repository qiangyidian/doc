package com.qiange.ragdemo.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条片段压缩决策。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompressionDecision {

    private Integer index;

    private Boolean keep;

    private String compressedContent;
}
