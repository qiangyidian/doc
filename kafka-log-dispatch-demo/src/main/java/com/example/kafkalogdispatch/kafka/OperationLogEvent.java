package com.example.kafkalogdispatch.kafka;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogEvent {

    private String operatorName;

    private String operationType;

    private String logLevel;

    private String content;

    private LocalDateTime eventTime;
}