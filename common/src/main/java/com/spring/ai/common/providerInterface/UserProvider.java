package com.spring.ai.common.providerInterface;

/**
 * 获取用户信息
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/14
 */
public interface UserProvider {

    /**
     * @return 获取当前登录用户id
     */
    Long getCurrentUserId();

    /**
     * @return 获取当前登录用户name
     */
    String getCurrentUserName();

}
