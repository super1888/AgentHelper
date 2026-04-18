package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillBatchConfigDTO {

    private Integer batchEnabled;

    private Integer importEnabled;

    private Integer exportEnabled;

    private List<String> importFormats;

    private List<String> exportFormats;

    private Integer tagBatchSupported;

    private Integer categoryBatchSupported;

    private Integer logicalDeleteEnabled;

    private Integer recycleEnabled;

    private Integer copyEnabled;
}
