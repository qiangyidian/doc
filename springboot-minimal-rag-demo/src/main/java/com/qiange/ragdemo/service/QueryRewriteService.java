package com.qiange.ragdemo.service;

import java.util.List;

/**
 * 查询改写服务。
 */
public interface QueryRewriteService {

    /**
     * 将用户问题改写为多路查询词。
     *
     * @param question 用户问题
     * @return 查询词列表
     */
    List<String> rewriteQueries(String question);
}
