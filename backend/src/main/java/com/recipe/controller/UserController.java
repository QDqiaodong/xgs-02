package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.dto.UserStatsDTO;
import com.recipe.entity.Recipe;
import com.recipe.mapper.FavoriteMapper;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final RecipeService recipeService;
    private final RecipeMapper recipeMapper;
    private final FavoriteMapper favoriteMapper;

    private static final Long DEFAULT_USER_ID = 1L;

    @GetMapping("/recipes")
    public Result<List<Recipe>> getUserRecipes() {
        return Result.success(recipeService.getUserRecipes());
    }

    @GetMapping("/profile")
    public Result<UserStatsDTO> getUserProfile() {
        UserStatsDTO stats = new UserStatsDTO();
        stats.setUserId(DEFAULT_USER_ID);
        stats.setNickname("美食达人");
        stats.setAvatar("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop");
        stats.setRecipeCount(recipeMapper.countPublishedRecipes());
        stats.setDraftCount(recipeMapper.countDrafts());
        stats.setFavoriteCount(favoriteMapper.countUserFavorites(DEFAULT_USER_ID));
        stats.setTotalLikes(recipeMapper.sumTotalLikes());
        stats.setViewCount(recipeMapper.sumTotalViews());
        return Result.success(stats);
    }
}