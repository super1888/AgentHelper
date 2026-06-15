package com.spring.ai.codehelper.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * codeHelper 模块配置。
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.code-helper")
public class CodeHelperProperties {

    private String workspaceRoot = System.getProperty("user.dir");

    private String defaultModelCode;

    private int maxSessionHistorySize = 200;

    private int maxToolOutputLength = 20000;

    private List<String> defaultAllowedCommands = List.of("mvn", "git", "java", "gradlew", "./mvnw", "mvnw", "cmd", "powershell");

}
