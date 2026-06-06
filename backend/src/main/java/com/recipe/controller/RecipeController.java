package com.recipe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.common.Result;
import com.recipe.dto.RecipeDTO;
import com.recipe.entity.Recipe;
import com.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/hot")
    public Result<List<Recipe>> getHotRecipes(
            @RequestParam(required = false, defaultValue = "total") String dimension) {
        return Result.success(recipeService.getHotRecipes(dimension));
    }

    @GetMapping
    public Result<Page<Recipe>> getRecipes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) Integer difficulty) {
        return Result.success(recipeService.getRecipePage(page, size, keyword, cuisine, scene, difficulty));
    }

    @GetMapping("/{id}")
    public Result<Recipe> getRecipeDetail(@PathVariable Long id) {
        return Result.success(recipeService.getRecipeDetail(id));
    }

    @PostMapping
    public Result<Recipe> createRecipe(@RequestBody RecipeDTO dto) {
        return Result.success(recipeService.createRecipe(dto));
    }

    @PutMapping("/{id}")
    public Result<Recipe> updateRecipe(@PathVariable Long id, @RequestBody RecipeDTO dto) {
        return Result.success(recipeService.updateRecipe(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteRecipe(@PathVariable Long id) {
        return Result.success(recipeService.deleteRecipe(id));
    }

    @PostMapping("/draft")
    public Result<Recipe> saveDraft(@RequestBody RecipeDTO dto) {
        return Result.success(recipeService.saveDraft(dto));
    }

    @GetMapping("/drafts")
    public Result<List<Recipe>> getDrafts() {
        return Result.success(recipeService.getDrafts());
    }
}