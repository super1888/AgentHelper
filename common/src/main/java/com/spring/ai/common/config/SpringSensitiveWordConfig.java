package com.spring.ai.common.config;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.spring.ai.common.bean.customWord.MyDdWordAllow;
import com.spring.ai.common.bean.customWord.MyDdWordDeny;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * class information
 *
 * @author zhouqi
 * @since 2026/3/31
 * @version 初次构建
 */
//@Configuration
//public class SpringSensitiveWordConfig {
//
//    @Resource
//    private MyDdWordAllow myDdWordAllow;
//
//    @Resource
//    private MyDdWordDeny myDdWordDeny;
//
//    /**
//     * 初始化引导类
//     * @return 初始化引导类
//     * @since 1.0.0
//     */
//    @Bean
//    public SensitiveWordBs sensitiveWordBs() {
//        SensitiveWordBs sensitiveWordBs = SensitiveWordBs.newInstance()
//                .wordAllow(WordAllows.chains(WordAllows.empty(), myDdWordAllow))
//                .wordDeny(myDdWordDeny)
//                // 各种其他配置
//                .init();
//
//        return sensitiveWordBs;
//    }
//
//}
