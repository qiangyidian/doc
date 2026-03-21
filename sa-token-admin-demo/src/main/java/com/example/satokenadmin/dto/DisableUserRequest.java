package com.example.satokenadmin.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DisableUserRequest {

    // 封禁时长，单位秒。
    @Min(value = 1, message = "封禁时长必须大于 0")
    private long disableSeconds;
}