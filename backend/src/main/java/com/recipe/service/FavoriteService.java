package com.recipe.service;

import com.recipe.entity.Recipe;

import java.util.List;

public interface FavoriteService {

    List<Recipe> getFavorites();

    boolean addFavorite(Long recipeId);

    boolean removeFavorite(Long recipeId);

    boolean removeFavoritesBatch(List<Long> recipeIds);
}