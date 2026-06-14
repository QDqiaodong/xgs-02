package com.recipe.service;

import com.recipe.dto.IngredientConflictDTO;
import com.recipe.dto.UserPreferenceDTO;
import java.util.List;

public interface IngredientConflictService {

    IngredientConflictDTO checkConflicts(Long recipeId, UserPreferenceDTO userPreference);

    IngredientConflictDTO checkConflictsByIngredients(List<String> ingredients, UserPreferenceDTO userPreference);
}
