package com.spring.ai.hooks.domain.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 内置目录模板对象
 */
@Data
@Builder
public class HookCatalogDTO {

    private String hookKey;

    private String hookName;

    private String description;

    private String hookType;

    private String hookStage;

    private String riskLevel;

    private String failStrategy;

    private String defaultConfigJson;

    private String defaultTestPayloadJson;

    private List<String> tags;
}
