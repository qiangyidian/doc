package com.qiange.ragdemo.dto;

import lombok.Data;

/**
 * 目录同步请求体。
 */
@Data
public class SyncKnowledgeDirectoryRequest {

    /**
     * 需要同步知识文件的本地目录绝对路径
     */
    private String directoryPath;

    /**
     * 目录中同步的知识所属的分类标签
     */
    private String category;

    /**
     * 是否递归扫描子目录中的知识文件进行同步
     */
    private Boolean recursive;
}
