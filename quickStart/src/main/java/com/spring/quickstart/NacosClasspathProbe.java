package com.spring.quickstart;

import com.alibaba.nacos.api.grpc.auto.RequestGrpc;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.Arrays;

/**
 * NacosClasspathProbe 类用于探测类加载信息，特别是打印类的来源位置和声明方法。
 * 这个类主要用于调试和验证类的加载来源，可以帮助开发者了解类是从哪里加载的。
 */
public class NacosClasspathProbe {

    /**
     * 主方法，程序的入口点
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 打印 RequestGrpc 类的加载信息
        printClassInfo(RequestGrpc.class);
        // 打印 RequestGrpc 类的所有声明方法
        printMethods(RequestGrpc.class);
    }

    /**
     * 打印指定类的加载信息
     * @param type 要检查的类对象
     */
    private static void printClassInfo(Class<?> type) {
        // 获取类的代码源信息
        CodeSource codeSource = type.getProtectionDomain().getCodeSource();
        System.out.println("Loaded class: " + type.getName());
        // 打印类来源位置，如果无法确定则显示 "unknown"
        System.out.println("From: " + (codeSource == null ? "unknown" : codeSource.getLocation()));
    }

    /**
     * 打印指定类的所有声明方法
     * @param type 要检查的类对象
     */
    private static void printMethods(Class<?> type) {
        System.out.println("Declared methods:");
        // 获取类中声明的所有方法，转换为字符串格式，排序后打印
        Arrays.stream(type.getDeclaredMethods())
                .map(Method::toGenericString)
                .sorted()
                .forEach(System.out::println);
    }
}
