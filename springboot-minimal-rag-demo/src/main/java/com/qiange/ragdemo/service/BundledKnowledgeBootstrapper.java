package com.qiange.ragdemo.service;

import com.qiange.ragdemo.config.RagProperties;
import com.qiange.ragdemo.loader.KnowledgeChunker;
import com.qiange.ragdemo.loader.LocalKnowledgeLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 启动时把项目自带知识库预热到本地向量表。
 *
 * 这样做的目标非常直接：
 * 1. 避开失效的外部 API Token
 * 2. 不破坏旧的 1024 维向量表示例数据
 * 3. 应用启动后就能直接问答，不需要先手动导入
 */
@Slf4j
@Component
public class BundledKnowledgeBootstrapper {

    // 用于校验表名合法性的正则表达式，防止 SQL 注入
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    // Spring 提供的 JDBC 模板，用于执行原生的 SQL 建表、建索引等操作
    private final JdbcTemplate jdbcTemplate;

    // 本地知识文件加载器，用于读取自带的 .md 或 .txt 文件
    private final LocalKnowledgeLoader localKnowledgeLoader;

    // 知识分块器，用于将长文本切分成适合向量化的短片段
    private final KnowledgeChunker knowledgeChunker;

    // 向量存储接口，将分块后的知识片段以及大模型生成的向量保存到数据库（这里是 PGVector）
    private final VectorStore vectorStore;

    // RAG 的业务配置属性，包含自带知识库的路径和默认分类等
    private final RagProperties ragProperties;

    // 向量表的名字，从 application.yml 中注入
    private final String vectorTableName;

    // 向量的维度大小（与使用的 Embedding 模型相匹配），从 application.yml 中注入
    private final Integer vectorDimensions;

    public BundledKnowledgeBootstrapper(
            JdbcTemplate jdbcTemplate,
            LocalKnowledgeLoader localKnowledgeLoader,
            KnowledgeChunker knowledgeChunker,
            VectorStore vectorStore,
            RagProperties ragProperties,
            @Value("${spring.ai.vectorstore.pgvector.table-name}") String vectorTableName,
            @Value("${spring.ai.vectorstore.pgvector.dimensions}") Integer vectorDimensions
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.localKnowledgeLoader = localKnowledgeLoader;
        this.knowledgeChunker = knowledgeChunker;
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
        this.vectorTableName = vectorTableName;
        this.vectorDimensions = vectorDimensions;
    }

    /**
     * 监听 Spring Boot 的 ApplicationReadyEvent 事件。
     * 在整个应用启动完毕，能够对外提供服务时，触发知识库的自动预热（导入）动作。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIfNecessary() {
        // 1. 校验配置中的表名是否合法，防止在拼接 SQL 时被注入
        validateIdentifier(vectorTableName);
        
        // 2. 确保底层的向量表及向量索引已经建立（针对 PostgreSQL 的 PGVector 扩展）
        ensureVectorTable();

        // 3. 查询向量表当前的数据量，判断是否已经有过数据
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + vectorTableName,
                Integer.class
        );

        // 如果表里已经有数据，则跳过自带知识库的预热导入，避免每次启动都重复跑
        if (rowCount != null && rowCount > 0) {
            log.info("Bundled knowledge bootstrap skipped because {} already contains {} rows", vectorTableName, rowCount);
            return;
        }

        // 4. 获取自带知识库所在的目录路径
        Path bootstrapPath = Path.of(ragProperties.getKnowledgeBase().getBootstrapPath());
        // 如果该目录不存在，则跳过
        if (!Files.exists(bootstrapPath)) {
            log.warn("Bundled knowledge bootstrap skipped because path does not exist: {}", bootstrapPath.toAbsolutePath());
            return;
        }

        // 5. 扫描自带知识库目录下的所有支持的文件，并加载、分块
        List<Document> chunkDocuments = loadBundledDocuments(bootstrapPath);
        // 如果目录里没有支持的文件，则跳过
        if (chunkDocuments.isEmpty()) {
            log.warn("Bundled knowledge bootstrap skipped because no supported files were found under {}", bootstrapPath.toAbsolutePath());
            return;
        }

        // 6. 调用 VectorStore，统一将所有分块好的文档及元数据、生成的向量插入到数据库中
        vectorStore.add(chunkDocuments);
        log.info("Bundled knowledge bootstrap loaded {} chunks into {}", chunkDocuments.size(), vectorTableName);
    }

    /**
     * 确保 PG 数据库中已经安装了必要的扩展、创建了存储向量的表，并建好了相关的向量索引。
     */
    private void ensureVectorTable() {
        // 创建 pgvector 扩展
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        // 创建 uuid 生成所需的扩展
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        // 创建存储文档和向量的表。表名和向量维度通过格式化字符串安全注入（前提是已做表名合法性校验）
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    content TEXT NOT NULL,
                    metadata JSON,
                    embedding VECTOR(%d)
                )
                """.formatted(vectorTableName, vectorDimensions));
        // 为向量列创建 hnsw 索引，以加速向量余弦相似度（vector_cosine_ops）检索
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS %s
                ON %s
                USING hnsw (embedding vector_cosine_ops)
                """.formatted(vectorTableName + "_embedding_hnsw_idx", vectorTableName));
    }

    /**
     * 遍历预置目录下的文件，并完成读取和切分工作。
     *
     * @param bootstrapPath 自带知识库所在的根目录
     * @return 分块完毕的知识片段列表
     */
    private List<Document> loadBundledDocuments(Path bootstrapPath) {
        try (Stream<Path> pathStream = Files.list(bootstrapPath)) {
            return pathStream
                    // 仅处理常规文件（忽略子目录等）
                    .filter(Files::isRegularFile)
                    // 过滤出受支持的文件格式（如 .md, .txt）
                    .filter(this::isSupportedFile)
                    // 按文件名排序，保证不同环境下处理顺序一致
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    // 核心：逐个加载文件并分块
                    .map(this::loadAndSplit)
                    // 将每个文件分出的一组小块（List<Document>），扁平化成一个大的 List<Document>
                    .flatMap(List::stream)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("扫描内置知识库目录失败：" + bootstrapPath.toAbsolutePath(), e);
        }
    }

    /**
     * 判断给定路径的文件是否属于当前支持处理的格式。
     */
    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".md") || fileName.endsWith(".txt");
    }

    /**
     * 内部辅助方法：调用本地加载器读取文件内容，然后直接喂给分块器切分成多个小 Document。
     */
    private List<Document> loadAndSplit(Path path) {
        Document originalDocument = localKnowledgeLoader.load(
                path.toString(),
                ragProperties.getKnowledgeBase().getBootstrapCategory(),
                null
        );
        return knowledgeChunker.split(originalDocument);
    }

    /**
     * 校验配置参数（如表名）是否只包含安全的字符，以防在拼接 SQL 执行时被恶意注入。
     */
    private void validateIdentifier(String identifier) {
        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("非法的向量表名配置：" + identifier);
        }
    }
}