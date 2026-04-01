package com.spring.ai.common.singleton;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/31
 */
public class Singleton {

    private volatile static Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }

}
