package com.spring.ai.agent.application.manager;

import com.spring.ai.agent.domain.request.KitchenRecipeRecommendRequest;
import com.spring.ai.agent.domain.response.KitchenRecipeRecommendResponse;
import com.spring.ai.agent.domain.response.KitchenRecipeRecommendResponse.RecipeItem;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.opencv.domain.request.ImageDetectRequest;
import com.spring.ai.opencv.domain.response.DetectionBoxResponse;
import com.spring.ai.opencv.domain.response.YoloDetectResponse;
import com.spring.ai.opencv.service.YoloDetectionService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Kitchen demo recipe recommendation manager.
 */
@Component
public class KitchenAgentApplicationManager {

    @Resource
    private YoloDetectionService yoloDetectionService;

    public KitchenRecipeRecommendResponse recommendRecipes(KitchenRecipeRecommendRequest request) {
        if (request == null || !StringUtils.hasText(request.getImageBase64())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "Image content cannot be empty");
        }
        ImageDetectRequest detectRequest = new ImageDetectRequest();
        detectRequest.setImageBase64(request.getImageBase64());
        detectRequest.setImageFormat(request.getImageFormat());
        detectRequest.setBusinessScene(request.getBusinessScene());

        YoloDetectResponse detectResponse = yoloDetectionService.detectKitchenIngredients(detectRequest);
        Set<String> ingredientSet = extractIngredientCodes(detectResponse.getDetections(), request.getExcludeIngredients());
        List<RecipeItem> recipes = buildRecipes(ingredientSet);
        return KitchenRecipeRecommendResponse.builder()
                .recognizedIngredients(new ArrayList<>(ingredientSet))
                .recipes(recipes)
                .build();
    }

    private Set<String> extractIngredientCodes(List<DetectionBoxResponse> detections, List<String> excludeIngredients) {
        Set<String> ingredientCodes = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(detections)) {
            for (DetectionBoxResponse detection : detections) {
                if (StringUtils.hasText(detection.getClassCode())) {
                    ingredientCodes.add(detection.getClassCode());
                }
            }
        }
        if (!CollectionUtils.isEmpty(excludeIngredients)) {
            ingredientCodes.removeAll(excludeIngredients);
        }
        return ingredientCodes;
    }

    private List<RecipeItem> buildRecipes(Set<String> ingredientSet) {
        List<RecipeItem> recipes = new ArrayList<>();
        if (ingredientSet.contains("EGG") && ingredientSet.contains("TOMATO")) {
            recipes.add(RecipeItem.builder()
                    .recipeName("Tomato Scrambled Eggs")
                    .score(0.95D)
                    .reason("Core ingredients are complete and suitable for a quick home dish")
                    .availableIngredients(List.of("EGG", "TOMATO", "SCALLION"))
                    .missingIngredients(List.of())
                    .optionalIngredients(List.of("SUGAR"))
                    .steps(List.of(
                            "Beat the eggs and set aside",
                            "Cut tomatoes into chunks",
                            "Cook eggs first, then stir-fry with tomatoes",
                            "Add scallion and season to finish"
                    ))
                    .tips(List.of("Add a small amount of sugar if you want a brighter tomato flavor"))
                    .difficulty("EASY")
                    .cookTimeMinutes(10)
                    .build());
            recipes.add(RecipeItem.builder()
                    .recipeName("Tomato Egg Soup")
                    .score(0.88D)
                    .reason("Current ingredients are enough for a simple soup")
                    .availableIngredients(List.of("EGG", "TOMATO"))
                    .missingIngredients(List.of())
                    .optionalIngredients(List.of("SCALLION", "SESAME_OIL"))
                    .steps(List.of(
                            "Boil tomato chunks until soft",
                            "Pour in egg mixture to form egg ribbons",
                            "Season with salt and serve"
                    ))
                    .tips(List.of("Scallion can be added before serving"))
                    .difficulty("EASY")
                    .cookTimeMinutes(12)
                    .build());
        }
        if (ingredientSet.contains("POTATO") && ingredientSet.contains("PORK")) {
            recipes.add(RecipeItem.builder()
                    .recipeName("Stir-fried Potato with Pork")
                    .score(0.92D)
                    .reason("Potato and pork make a stable home-style stir-fry")
                    .availableIngredients(List.of("POTATO", "PORK"))
                    .missingIngredients(List.of("SCALLION"))
                    .optionalIngredients(List.of("PEPPER", "GARLIC"))
                    .steps(List.of(
                            "Slice potatoes and soak briefly",
                            "Marinate pork slices in advance",
                            "Cook pork first, then add potatoes",
                            "Season and stir-fry until cooked through"
                    ))
                    .tips(List.of("Do not cut potato slices too thick"))
                    .difficulty("MEDIUM")
                    .cookTimeMinutes(18)
                    .build());
        }
        if (ingredientSet.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "No recognizable ingredients were found");
        }
        if (recipes.isEmpty()) {
            recipes.add(RecipeItem.builder()
                    .recipeName("General Home Cooking Suggestion")
                    .score(0.70D)
                    .reason("Some ingredients were recognized, but no preset recipe matched exactly")
                    .availableIngredients(new ArrayList<>(ingredientSet))
                    .missingIngredients(List.of("Add eggs or scallion/garlic for better recipe matches"))
                    .optionalIngredients(List.of())
                    .steps(List.of(
                            "Group ingredients by protein and vegetables",
                            "Prefer simple stir-fry or soup combinations",
                            "Season with basic kitchen condiments"
                    ))
                    .tips(List.of("Adding a few basic condiments will improve the next recommendation"))
                    .difficulty("EASY")
                    .cookTimeMinutes(15)
                    .build());
        }
        return recipes;
    }
}
