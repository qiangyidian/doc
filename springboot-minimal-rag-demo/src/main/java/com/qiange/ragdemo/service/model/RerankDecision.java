package com.qiange.ragdemo.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条片段精排决策。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RerankDecision {

    private Integer index;

    private Double score;

    private Boolean keep;

    private String reason;
}
