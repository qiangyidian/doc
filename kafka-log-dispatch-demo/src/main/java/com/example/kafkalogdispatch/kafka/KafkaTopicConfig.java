package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic operationLogTopic() {
        return new NewTopic(Constants.OPERATION_LOG_TOPIC, 1, (short) 1);
    }
}