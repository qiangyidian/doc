package com.qiange.ragdemo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第二阶段文档台账实体。
 */
@Data
public class KnowledgeDocumentEntity {

    private Long id;

    private String fileName;

    private String filePath;

    private String fileHash;

    private String category;

    private String status;

    private String remark;

    private LocalDateTime lastSyncAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
