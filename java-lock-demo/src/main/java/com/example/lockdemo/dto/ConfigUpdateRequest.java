package com.example.lockdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfigUpdateRequest {

    @NotBlank(message = "配置key不能为空")
    private String key;

    @NotBlank(message = "配置value不能为空")
    private String value;
}