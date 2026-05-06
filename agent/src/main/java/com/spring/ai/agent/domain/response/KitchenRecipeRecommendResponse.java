package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 厨房菜谱推荐响应。
 */
@Data
@Builder
public class KitchenRecipeRecommendResponse {

    private List<String> recognizedIngredients;
    private List<RecipeItem> recipes;

    @Data
    @Builder
    public static class RecipeItem {

        private String recipeName;
        private Double score;
        private String reason;
        private List<String> availableIngredients;
        private List<String> missingIngredients;
        private List<String> optionalIngredients;
        private List<String> steps;
        private List<String> tips;
        private String difficulty;
        private Integer cookTimeMinutes;
    }
}
