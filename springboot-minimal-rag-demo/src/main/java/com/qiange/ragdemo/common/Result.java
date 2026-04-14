package com.qiange.ragdemo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回体包装类。
 * 用于统一后端接口返回的数据格式，方便前端或调用方统一解析。
 *
 * 这里不追求复杂，只做最小可用：
 * - success：标识请求是否业务上成功
 * - message：给前端或调用方看的说明信息或错误信息
 * - data：真正的业务数据荷载（Payload）
 *
 * 为什么教程型项目也建议统一返回？
 * 因为你后面调试导入接口、问答接口时，
 * 能更直观看出到底是业务失败还是系统异常。
 */
@Data // 自动生成所有字段的 getter/setter、toString()、equals() 和 hashCode()
@NoArgsConstructor // 自动生成无参构造函数（为了框架反序列化需要）
@AllArgsConstructor // 自动生成包含所有字段的构造函数
public class Result<T> {

    // 标志位：操作是否成功
    private boolean success;

    // 提示信息：通常用于携带成功描述或错误信息
    private String message;

    // 实际业务数据
    private T data;

    /**
     * 静态工厂方法：构建一个不带自定义消息，仅包含业务数据的成功响应对象。
     * @param data 要返回给客户端的具体业务数据
     * @param <T> 数据类型泛型
     * @return 成功状态的 Result 对象
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, "success", data);
    }

    /**
     * 静态工厂方法：构建一个包含自定义消息和业务数据的成功响应对象。
     * @param message 成功提示消息
     * @param data 要返回给客户端的具体业务数据
     * @param <T> 数据类型泛型
     * @return 成功状态的 Result 对象
     */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(true, message, data);
    }

    /**
     * 静态工厂方法：构建一个包含错误提示信息的失败响应对象。
     * 当发生异常或业务校验失败时使用此方法。
     * @param message 失败的原因或提示信息
     * @param <T> 数据类型泛型
     * @return 失败状态且没有数据的 Result 对象
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(false, message, null);
    }
}
