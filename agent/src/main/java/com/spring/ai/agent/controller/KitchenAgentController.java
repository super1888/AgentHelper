package com.spring.ai.agent.controller;

import com.spring.ai.agent.application.manager.KitchenAgentApplicationManager;
import com.spring.ai.agent.domain.request.KitchenRecipeRecommendRequest;
import com.spring.ai.agent.domain.response.KitchenRecipeRecommendResponse;
import com.spring.ai.common.web.ApiResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 厨房帮手控制器。
 */
@RestController
@RequestMapping("/agents/kitchen")
public class KitchenAgentController {

    @Resource
    private KitchenAgentApplicationManager kitchenAgentApplicationManager;

    @PostMapping("/recipe/recommend")
    public ApiResponse<KitchenRecipeRecommendResponse> recommendRecipes(
            @Valid @RequestBody KitchenRecipeRecommendRequest request
    ) {
        return ApiResponse.success("推荐成功", kitchenAgentApplicationManager.recommendRecipes(request));
    }
}
