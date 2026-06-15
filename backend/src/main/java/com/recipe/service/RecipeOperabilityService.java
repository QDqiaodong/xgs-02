package com.recipe.service;

import com.recipe.dto.RecipeOperabilityAssessmentDTO;

public interface RecipeOperabilityService {

    RecipeOperabilityAssessmentDTO assessRecipe(Long recipeId);

    RecipeOperabilityAssessmentDTO assessRecipeByContent(String ingredientsJson, String stepsJson, Integer cookTime);
}
