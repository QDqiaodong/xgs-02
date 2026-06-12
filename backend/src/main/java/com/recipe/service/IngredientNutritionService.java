package com.recipe.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.dto.RecipeNutritionDTO;
import com.recipe.entity.IngredientNutrition;

import java.util.List;

public interface IngredientNutritionService {

    Page<IngredientNutrition> getNutritionPage(Integer page, Integer size, String keyword, String category);

    IngredientNutrition getNutritionById(Long id);

    IngredientNutrition getNutritionByName(String name);

    List<IngredientNutrition> getAllNutritions();

    List<String> getAllCategories();

    IngredientNutrition createNutrition(IngredientNutrition nutrition);

    IngredientNutrition updateNutrition(Long id, IngredientNutrition nutrition);

    boolean deleteNutrition(Long id);

    RecipeNutritionDTO calculateRecipeNutrition(String ingredientsJson);

    RecipeNutritionDTO calculateRecipeNutritionById(Long recipeId);
}
