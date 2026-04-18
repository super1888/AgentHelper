package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillMarketplaceConfigDTO {

    private Integer marketplaceEnabled;

    private Integer thirdPartyUploadEnabled;

    private Integer reviewRequired;

    private Integer storeVisible;

    private String subscriptionMode;
}
