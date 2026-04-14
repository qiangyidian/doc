package com.qiange.ragdemo.handler;

import com.qiange.ragdemo.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局的异常处理拦截器（Global Exception Handler）。
 * 用于集中捕获并处理从 Controller 抛出的、或是 Service 层一直往上抛的那些异常。
 * 它可以将底层抛出来的异常转化为统一样式（Result.fail()）的 JSON 返回给前端。
 *
 * 为什么即便是教程型项目也强烈建议添加这个类？
 * 因为在学习和练习导入知识的时候，经常会触发诸如：文件路径不存在、重复导入同一个文件、API Key 未配置等各类问题。
 * 如果没有这一层统一收口，异常直接抛出，前台收到的就是大段堆栈或默认的乱码结构，既不利于查看也不利于迅速定位错误发生的原因。
 */
@RestControllerAdvice // 声明为一个适用于所有 @RestController 的增强类，它会自动将方法返回值处理为 JSON
public class GlobalExceptionHandler {

    /**
     * 捕获并处理针对参数校验或业务规则不通过引发的 IllegalArgumentException 异常。
     * 这是一种比较常见的针对用户输入的显式异常（例如：上传文件为空，重复导入）。
     * 它可以把封装好的文字直接包装回 success: false 的状态，提醒用户并提供友好的指引。
     *
     * @param e 具体的非法参数异常对象
     * @return 统一封装的失败响应实体，其中包含该异常携带的失败原因
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.fail(e.getMessage());
    }

    /**
     * 作为异常处理的兜底防线，用来捕获那些未被显式定义处理方法的所有受检异常和运行时异常 (Exception)。
     * 例如在读取文件时发生的 IO 异常、或者大模型网络超时、又或者是未知系统内部错误。
     * 这样就不会导致程序意外崩溃或接口没有响应。
     *
     * @param e 捕获到的通用系统级异常对象
     * @return 统一封装的失败响应实体，并在 message 前增加 "系统异常：" 的字样以便区分
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.fail("系统异常：" + e.getMessage());
    }
}
