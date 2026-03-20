package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.repository.AlertLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AlertLogConsumer {

    private final AlertLogRepository alertLogRepository;

    public AlertLogConsumer(AlertLogRepository alertLogRepository) {
        this.alertLogRepository = alertLogRepository;
    }

    @KafkaListener(topics = Constants.OPERATION_LOG_TOPIC, groupId = Constants.ALERT_GROUP)
    public void listen(OperationLogEvent event) {
        AlertLog alertLog = new AlertLog();
        alertLog.setLogLevel(event.getLogLevel());
        alertLog.setContent(event.getContent());
        alertLog.setAlertStatus(
                ("ERROR".equalsIgnoreCase(event.getLogLevel()) || "WARN".equalsIgnoreCase(event.getLogLevel()))
                        ? "NEED_ALERT"
                        : "IGNORE"
        );
        alertLog.setCreateTime(LocalDateTime.now());
        alertLogRepository.save(alertLog);
    }
}