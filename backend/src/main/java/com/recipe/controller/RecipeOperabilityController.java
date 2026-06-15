package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.dto.RecipeOperabilityAssessmentDTO;
import com.recipe.service.RecipeOperabilityService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recipe-operability")
@RequiredArgsConstructor
public class RecipeOperabilityController {

    private final RecipeOperabilityService recipeOperabilityService;

    @GetMapping("/assess/recipe/{recipeId}")
    public Result<RecipeOperabilityAssessmentDTO> assessRecipe(@PathVariable Long recipeId) {
        RecipeOperabilityAssessmentDTO result = recipeOperabilityService.assessRecipe(recipeId);
        return Result.success(result);
    }

    @PostMapping("/assess/custom")
    public Result<RecipeOperabilityAssessmentDTO> assessCustomRecipe(@RequestBody AssessRequest request) {
        RecipeOperabilityAssessmentDTO result = recipeOperabilityService.assessRecipeByContent(
                request.getIngredients(),
                request.getSteps(),
                request.getCookTime()
        );
        return Result.success(result);
    }

    @Data
    public static class AssessRequest {
        private String ingredients;
        private String steps;
        private Integer cookTime;
    }
}
