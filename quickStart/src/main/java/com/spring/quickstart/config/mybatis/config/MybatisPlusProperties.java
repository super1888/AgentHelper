package com.spring.quickstart.config.mybatis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis-Plus 通用开关配置。
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "app.mybatis-plus")
public class MybatisPlusProperties {

    /**
     * 是否开启乐观锁插件，默认关闭。
     */
    private boolean enableOptimisticLocker = false;

    /**
     * 是否开启 SQL 打印，默认关闭。
     */
    private boolean enableSqlLog = false;

}
