package com.example.logintoken.common;

public final class Constants {

    private Constants() {
    }

    public static final String LOGIN_TOKEN_PREFIX = "login:token:";

    /**
     * Token 默认过期时间，单位：分钟。
     */
    public static final long TOKEN_EXPIRE_MINUTES = 30L;
}