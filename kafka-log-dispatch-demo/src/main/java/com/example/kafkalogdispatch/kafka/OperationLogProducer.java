package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperationLogProducer {

    private final KafkaTemplate<String, OperationLogEvent> kafkaTemplate;

    public OperationLogProducer(KafkaTemplate<String, OperationLogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(OperationLogEvent event) {
        kafkaTemplate.send(Constants.OPERATION_LOG_TOPIC, event.getOperationType(), event);
    }
}