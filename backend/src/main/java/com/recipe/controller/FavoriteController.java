package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.entity.Recipe;
import com.recipe.service.FavoriteService;
import com.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Result<List<Recipe>> getFavorites() {
        return Result.success(favoriteService.getFavorites());
    }

    @PostMapping
    public Result<Boolean> addFavorite(@RequestBody Map<String, Long> body) {
        Long recipeId = body.get("recipeId");
        return Result.success(favoriteService.addFavorite(recipeId));
    }

    @DeleteMapping("/{recipeId}")
    public Result<Boolean> removeFavorite(@PathVariable Long recipeId) {
        return Result.success(favoriteService.removeFavorite(recipeId));
    }

    @DeleteMapping("/batch")
    public Result<Boolean> removeFavoritesBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        return Result.success(favoriteService.removeFavoritesBatch(ids));
    }
}