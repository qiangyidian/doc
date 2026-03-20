package com.example.kafkalogdispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogDispatchResponse {

    private Long logId;

    private String message;
}