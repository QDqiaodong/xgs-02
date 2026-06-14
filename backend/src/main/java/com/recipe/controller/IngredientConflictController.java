package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.dto.IngredientConflictDTO;
import com.recipe.dto.UserPreferenceDTO;
import com.recipe.service.IngredientConflictService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredient-conflict")
@RequiredArgsConstructor
public class IngredientConflictController {

    private final IngredientConflictService ingredientConflictService;

    @PostMapping("/check/recipe/{recipeId}")
    public Result<IngredientConflictDTO> checkRecipeConflicts(
            @PathVariable Long recipeId,
            @RequestBody(required = false) UserPreferenceDTO userPreference) {
        IngredientConflictDTO result = ingredientConflictService.checkConflicts(recipeId, userPreference);
        return Result.success(result);
    }

    @PostMapping("/check/ingredients")
    public Result<IngredientConflictDTO> checkIngredientConflicts(
            @RequestBody CheckRequest request) {
        IngredientConflictDTO result = ingredientConflictService.checkConflictsByIngredients(
                request.getIngredients(),
                request.getUserPreference()
        );
        return Result.success(result);
    }

    @Data
    public static class CheckRequest {
        private List<String> ingredients;
        private UserPreferenceDTO userPreference;
    }
}
