package com.qiange.ragdemo.mapper;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识文档台账表的 MyBatis Mapper 接口。
 * 负责定义访问和操作 `knowledge_document` 表的底层方法。
 * 这里的方法名和参数会与相应的 XML 映射文件绑定，以执行具体的 SQL 语句。
 */
@Mapper // MyBatis 注解，标记此接口为一个持久层组件，Spring Boot 会在启动时扫描并创建代理实现
public interface KnowledgeDocumentMapper {

    /**
     * 根据文档的哈希标识（doc_id）查询数据库，判断该文件是否已经被导入过。
     *
     * @param documentId 文件内容计算出的唯一摘要（MD5等）
     * @return 存在的话返回该文档对应的数据库台账实体对象，不存在则返回 null
     */
    KnowledgeDocumentEntity selectByDocumentId(@Param("documentId") String documentId);

    /**
     * 插入一条新的知识导入台账记录。
     * 常用于占位操作，将文件的状态标记为 PENDING（处理中）。
     * 执行插入后，传入的实体对象中的 id 字段将被自动填充为主键值。
     *
     * @param entity 包含文件基本信息（不含分块结果）的实体对象
     * @return 影响的行数，通常为 1 代表成功
     */
    int insert(KnowledgeDocumentEntity entity);

    /**
     * 当该文件的内容已被成功分块并入库向量表后（或者抛出异常时），
     * 使用此方法更新当前任务的最新状态和它被切分出来的片段总数。
     *
     * @param id 该文件记录在台账表中的自增主键
     * @param status 当前处理的最新状态，如 "IMPORTED" 成功、 "FAILED" 失败
     * @param chunkCount 切分后该文件所产生的小段数（失败时可更新为0）
     * @return 影响的行数，通常为 1 代表更新成功
     */
    int updateStatusAndChunkCount(@Param("id") Long id,
                                  @Param("status") String status,
                                  @Param("chunkCount") Integer chunkCount);
}
