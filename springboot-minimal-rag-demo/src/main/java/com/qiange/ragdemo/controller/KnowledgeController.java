package com.qiange.ragdemo.controller;

import com.qiange.ragdemo.common.Result;
import com.qiange.ragdemo.dto.ImportLocalFileRequest;
import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryRequest;
import com.qiange.ragdemo.dto.SyncKnowledgeDirectoryResponse;
import com.qiange.ragdemo.service.KnowledgeDirectorySyncService;
import com.qiange.ragdemo.service.KnowledgeImportFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识管理接口。
 * 提供知识导入、目录同步等功能。
 */
@RestController // 声明为 REST 控制器，返回 JSON 响应
@RequestMapping("/api/knowledge") // 定义此控制器的基础请求路径
@RequiredArgsConstructor // Lombok 注解，自动生成包含 final 字段的构造函数
public class KnowledgeController {

    // 知识导入门面服务，负责协调文件读取、切分、以及存储的整套流程
    private final KnowledgeImportFacade knowledgeImportFacade;

    // 知识目录同步服务，负责扫描指定目录并同步知识文件
    private final KnowledgeDirectorySyncService knowledgeDirectorySyncService;

    /**
     * 接收用户的导入请求，解析本地文件并将其入库为知识片段。
     *
     * @param request 包含文件路径、文件分类及标题的请求对象（已通过 @Valid 校验确保非空参数）
     * @return 统一格式的结果体，包含成功消息与实际拆分的片段数
     */
    @PostMapping("/import") // 映射 HTTP POST 请求到 /api/knowledge/import
    public Result<String> importLocalFile(@Valid @RequestBody ImportLocalFileRequest request) {
        // 调用 Service 进行实际的文件读取、分块与入库操作，并返回最终分出的代码片段总数
        Integer chunkCount = knowledgeImportFacade.importLocalFile(request);
        // 返回成功提示给调用端
        return Result.ok("知识导入成功", "共切分并写入 " + chunkCount + " 个知识片段");
    }

    /**
     * 同步指定目录下的知识文件到知识库。
     *
     * @param request 包含目录路径、分类和是否递归同步的请求对象
     * @return 统一格式的结果体，包含同步统计信息
     */
    @PostMapping("/sync-directory") // 映射 HTTP POST 请求到 /api/knowledge/sync-directory
    public Result<SyncKnowledgeDirectoryResponse> syncDirectory(@RequestBody SyncKnowledgeDirectoryRequest request) {
        // 调用服务执行目录同步操作
        SyncKnowledgeDirectoryResponse response = knowledgeDirectorySyncService.syncDirectory(request);
        // 返回同步结果
        return Result.ok("目录同步完成", response);
    }
}
