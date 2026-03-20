package com.example.kafkalogdispatch.dto;

import lombok.Data;

@Data
public class SendLogRequest {

    private String operatorName;

    private String operationType;

    private String logLevel;

    private String content;
}