
package com.example.kafkalogdispatch.service.impl;

import com.example.kafkalogdispatch.dto.LogDispatchResponse;
import com.example.kafkalogdispatch.dto.SendLogRequest;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.entity.AuditLog;
import com.example.kafkalogdispatch.entity.OperationLog;
import com.example.kafkalogdispatch.kafka.OperationLogEvent;
import com.example.kafkalogdispatch.kafka.OperationLogProducer;
import com.example.kafkalogdispatch.repository.AlertLogRepository;
import com.example.kafkalogdispatch.repository.AuditLogRepository;
import com.example.kafkalogdispatch.repository.OperationLogRepository;
import com.example.kafkalogdispatch.service.LogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    private final OperationLogRepository operationLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final AlertLogRepository alertLogRepository;
    private final OperationLogProducer operationLogProducer;

    public LogServiceImpl(OperationLogRepository operationLogRepository,
                          AuditLogRepository auditLogRepository,
                          AlertLogRepository alertLogRepository,
                          OperationLogProducer operationLogProducer) {
        this.operationLogRepository = operationLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.alertLogRepository = alertLogRepository;
        this.operationLogProducer = operationLogProducer;
    }

    @Override
    public LogDispatchResponse sendLog(SendLogRequest request) {
        OperationLog operationLog = new OperationLog();
        operationLog.setOperatorName(request.getOperatorName());
        operationLog.setOperationType(request.getOperationType());
        operationLog.setLogLevel(request.getLogLevel());
        operationLog.setContent(request.getContent());
        operationLog.setCreateTime(LocalDateTime.now());
        operationLogRepository.save(operationLog);

        OperationLogEvent event = new OperationLogEvent();
        event.setOperatorName(request.getOperatorName());
        event.setOperationType(request.getOperationType());
        event.setLogLevel(request.getLogLevel());
        event.setContent(request.getContent());
        event.setEventTime(LocalDateTime.now());
        operationLogProducer.send(event);

        return new LogDispatchResponse(operationLog.getId(), "日志已发送到 Kafka");
    }

    @Override
    public List<AuditLog> listAuditLogs() {
        return auditLogRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<AlertLog> listAlertLogs() {
        return alertLogRepository.findAllByOrderByIdDesc();
    }
}