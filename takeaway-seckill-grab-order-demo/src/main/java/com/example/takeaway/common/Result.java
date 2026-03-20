package com.example.takeaway.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Data 会自动生成 getter、setter、toString 等方法。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    // code 用来表示接口状态码。
    private Integer code;

    // message 用来描述请求结果。
    private String message;

    // data 才是真正的业务数据。
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}