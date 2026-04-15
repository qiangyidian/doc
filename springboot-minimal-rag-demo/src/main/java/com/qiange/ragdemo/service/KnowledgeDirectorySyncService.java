package com.qiange.ragdemo.service;

import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryRequest;
import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryResponse;

/**
 * 知识目录同步服务。
 */
public interface KnowledgeDirectorySyncService {

    /**
     * 执行指定目录的文件扫描和同步。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    SyncKnowledgeDirectoryResponse syncDirectory(SyncKnowledgeDirectoryRequest request);
}
