package com.qiange.ragdemo.repository;

import com.qiange.ragdemo.common.RagConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 向量存储清理仓库。
 *
 * 用于在文件发生变更、重新同步之前，把这篇文档原来在向量库中切出的那些片段先删掉，
 * 避免同一个文件改了几次之后，向量库里存在大量的冗余过时块。
 */
@Repository
@RequiredArgsConstructor
public class VectorStoreCleanupRepository {

    // 原生 JdbcTemplate，用于执行简单的 Delete SQL
    private final JdbcTemplate jdbcTemplate;

    // 获取当前应用使用的向量表名称
    @Value("${spring.ai.vectorstore.pgvector.table-name}")
    private String vectorTableName;

    /**
     * 根据文档的绝对路径，从向量表中物理删除其所有的片段行。
     * 匹配依据是当初导入时强绑定的 "sourcePath" 元数据。
     *
     * @param sourcePath 待删除片段的原始文件绝对路径
     * @return 实际从向量表里删掉的片段（行）数
     */
    public int deleteBySourcePath(String sourcePath) {
        // 构建删除 SQL，使用常量定义的元数据键名以保持一致性
        String sql = """
                delete from %s
                where metadata ->> '%s' = ?
                """.formatted(vectorTableName, RagConstants.METADATA_SOURCE_PATH);
        // 执行删除并返回影响的行数
        return jdbcTemplate.update(sql, sourcePath);
    }
}
