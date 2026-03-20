package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import com.example.kafkalogdispatch.entity.AuditLog;
import com.example.kafkalogdispatch.repository.AuditLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditLogConsumer {

    private final AuditLogRepository auditLogRepository;

    public AuditLogConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @KafkaListener(topics = Constants.OPERATION_LOG_TOPIC, groupId = Constants.AUDIT_GROUP)
    public void listen(OperationLogEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorName(event.getOperatorName());
        auditLog.setOperationType(event.getOperationType());
        auditLog.setContent(event.getContent());
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}