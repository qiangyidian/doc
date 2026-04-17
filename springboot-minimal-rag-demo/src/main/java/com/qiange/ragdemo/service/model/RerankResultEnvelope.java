package com.qiange.ragdemo.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 精排结构化输出包裹体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RerankResultEnvelope {

    private List<RerankDecision> results;
}
