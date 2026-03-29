package com.spring.quickstartdashscope.otherTests;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/26
 */
@SpringBootTest
public class OtherTest {

    @Test
    public void test() {

        System.out.println(testInfo());

    }

    private String testInfo() {
        try {
            throw new RuntimeException("1");
        } catch (Exception e) {
            return "2";
        } finally {
            return "3";
        }

    }



}
