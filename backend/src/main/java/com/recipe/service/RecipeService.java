package com.recipe.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.dto.RecipeDTO;
import com.recipe.entity.Recipe;

import java.util.List;

public interface RecipeService {

    List<Recipe> getHotRecipes();

    List<Recipe> getHotRecipes(String dimension);

    Page<Recipe> getRecipePage(Integer page, Integer size, String keyword, String cuisine, String scene, Integer difficulty);

    Recipe getRecipeDetail(Long id);

    Recipe createRecipe(RecipeDTO dto);

    Recipe updateRecipe(Long id, RecipeDTO dto);

    boolean deleteRecipe(Long id);

    Recipe saveDraft(RecipeDTO dto);

    List<Recipe> getDrafts();

    List<Recipe> getUserRecipes();
}