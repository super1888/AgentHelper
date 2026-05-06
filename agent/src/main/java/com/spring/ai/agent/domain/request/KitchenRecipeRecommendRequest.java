package com.spring.ai.agent.domain.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

/**
 * 厨房菜谱推荐请求。
 */
@Data
public class KitchenRecipeRecommendRequest {

    @NotBlank(message = "图片内容不能为空")
    private String imageBase64;

    @NotBlank(message = "图片格式不能为空")
    private String imageFormat;

    private String businessScene = "KITCHEN_ASSISTANT";

    private String userPrompt;

    private String preferredTaste;

    private List<String> excludeIngredients;
}
