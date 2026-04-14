package com.qiange.ragdemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 项目公共 Bean 配置类。
 * 用于定义和向 Spring 容器注册一些核心组件，或启用自定义的配置属性类。
 *
 * 这里我们故意保持克制，只注册最关键的 Bean：
 * - RagProperties：启用并读取我们在 application.yml 中自定义的配置
 * - ChatClient：后续问答生成时直接使用，负责跟底层的大语言模型交互
 *
 * 为什么不在这里手写 EmbeddingModel 和 VectorStore 的初始化代码？
 * 因为我们依赖了 Spring AI Starter（比如 spring-ai-openai-spring-boot-starter），
 * 它已经根据 yml 里的配置自动帮我们装配好这些底层实现类了。
 * 最小项目优先利用框架的自动配置能力，降低初学者的理解门槛。
 */
@Configuration // 声明这是一个 Spring 配置类，在启动时会被扫描并执行
@EnableConfigurationProperties(RagProperties.class) // 使带有 @ConfigurationProperties 的配置类生效并注入到容器中
public class RagBeanConfig {

    /**
     * 构建并注册一个 ChatClient 的实例。
     * 它是 Spring AI 提供的高层 API 门面，用来简化给大模型发 prompt 及接收回复的操作。
     *
     * @param chatClientBuilder Spring AI 自动配置好的构建器，内部已绑定了底层语言模型（如 OpenAiChatModel 等）
     * @return 构建完成的 ChatClient，可以在我们的服务类（比如 RagChatServiceImpl）中直接注入使用
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
