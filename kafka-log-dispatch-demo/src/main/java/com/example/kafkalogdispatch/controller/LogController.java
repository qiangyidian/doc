package com.example.kafkalogdispatch.controller;

import com.example.kafkalogdispatch.common.Result;
import com.example.kafkalogdispatch.dto.LogDispatchResponse;
import com.example.kafkalogdispatch.dto.SendLogRequest;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.entity.AuditLog;
import com.example.kafkalogdispatch.service.LogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/send")
    public Result<LogDispatchResponse> send(@RequestBody SendLogRequest request) {
        return Result.success(logService.sendLog(request));
    }

    @GetMapping("/audit")
    public Result<List<AuditLog>> auditLogs() {
        return Result.success(logService.listAuditLogs());
    }

    @GetMapping("/alert")
    public Result<List<AlertLog>> alertLogs() {
        return Result.success(logService.listAlertLogs());
    }
}