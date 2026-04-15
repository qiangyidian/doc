package com.qiange.ragdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目录同步结果响应体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncKnowledgeDirectoryResponse {

    /**
     * 在指定目录中成功扫描到的符合条件文件总数
     */
    private Integer scannedFiles;

    /**
     * 全新导入向量数据库及系统存储的文件数
     */
    private Integer importedFiles;

    /**
     * 内容已更新并重新解析存入系统中的文件数
     */
    private Integer updatedFiles;

    /**
     * 因为未发生内容变更或不满足条件而被跳过导入的文件数
     */
    private Integer skippedFiles;

    /**
     * 在解析、读取或向向量库导入过程中遇到错误失败的文件数
     */
    private Integer failedFiles;
}
