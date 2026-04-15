package com.qiange.ragdemo.mapper;

import com.qiange.ragdemo.entity.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 第二阶段文档台账 Mapper。
 * 负责定义访问和操作 `kb_document` 表的底层方法。
 * 这里的方法名和参数会与相应的 XML 映射文件绑定，以执行具体的 SQL 语句。
 */
@Mapper // MyBatis 注解，标记此接口为一个持久层组件，Spring Boot 会在启动时扫描并创建代理实现
public interface KnowledgeDocumentMapper {

    /**
     * 根据文档的内容哈希查询数据库，判断该内容是否已经被导入过。
     *
     * @param fileHash 文件内容计算出的唯一摘要（MD5等）
     * @return 存在的话返回该文档对应的数据库台账实体对象，不存在则返回 null
     */
    KnowledgeDocumentEntity selectByFileHash(@Param("fileHash") String fileHash);

    /**
     * 根据文档的绝对路径查询数据库，判断该文件是否已经被扫描处理过。
     * 结合哈希可用于判断该文件是被修改了还是完全未变。
     *
     * @param filePath 文件的绝对物理路径
     * @return 存在的话返回该文档对应的数据库台账实体对象，不存在则返回 null
     */
    KnowledgeDocumentEntity selectByFilePath(@Param("filePath") String filePath);

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
     * 在增量同步的中间状态更新，主要用来把旧哈希替换成新哈希，
     * 并且把状态置回 PENDING。
     *
     * @param id       自增主键
     * @param fileHash 最新的文件内容哈希
     * @param status   最新的处理状态（通常为 PENDING）
     * @param remark   备注信息（例如：检测到内容变更...）
     * @return 影响的行数
     */
    int updateBeforeResync(@Param("id") Long id,
                           @Param("fileHash") String fileHash,
                           @Param("status") String status,
                           @Param("remark") String remark);

    /**
     * 当该文件的内容已被成功分块并入库向量表后，更新状态为 IMPORTED。
     * 此操作同时会刷新 last_sync_at 时间戳。
     *
     * @param id     自增主键
     * @param status 最新的处理状态（通常为 IMPORTED）
     * @param remark 备注信息（例如：同步成功，切分了 xx 片段）
     * @return 影响的行数
     */
    int updateAfterSync(@Param("id") Long id,
                        @Param("status") String status,
                        @Param("remark") String remark);

    /**
     * 当该文件解析或向向量表导入发生异常失败时，将状态更新为 FAILED，便于后续重试。
     * 失败情况不刷新 last_sync_at。
     *
     * @param id     自增主键
     * @param status 最新的处理状态（通常为 FAILED）
     * @param remark 备注信息（通常为异常报错）
     * @return 影响的行数
     */
    int updateFailed(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("remark") String remark);
}
