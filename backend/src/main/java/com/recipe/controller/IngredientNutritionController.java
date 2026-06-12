package com.recipe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.common.Result;
import com.recipe.dto.RecipeNutritionDTO;
import com.recipe.entity.IngredientNutrition;
import com.recipe.service.IngredientNutritionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ingredient-nutrition")
@RequiredArgsConstructor
public class IngredientNutritionController {

    private final IngredientNutritionService nutritionService;

    @GetMapping
    public Result<Page<IngredientNutrition>> getNutritions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return Result.success(nutritionService.getNutritionPage(page, size, keyword, category));
    }

    @GetMapping("/all")
    public Result<List<IngredientNutrition>> getAllNutritions() {
        return Result.success(nutritionService.getAllNutritions());
    }

    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(nutritionService.getAllCategories());
    }

    @GetMapping("/{id}")
    public Result<IngredientNutrition> getNutritionById(@PathVariable Long id) {
        return Result.success(nutritionService.getNutritionById(id));
    }

    @GetMapping("/name/{name}")
    public Result<IngredientNutrition> getNutritionByName(@PathVariable String name) {
        return Result.success(nutritionService.getNutritionByName(name));
    }

    @PostMapping
    public Result<IngredientNutrition> createNutrition(@RequestBody IngredientNutrition nutrition) {
        return Result.success(nutritionService.createNutrition(nutrition));
    }

    @PutMapping("/{id}")
    public Result<IngredientNutrition> updateNutrition(@PathVariable Long id, @RequestBody IngredientNutrition nutrition) {
        return Result.success(nutritionService.updateNutrition(id, nutrition));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteNutrition(@PathVariable Long id) {
        return Result.success(nutritionService.deleteNutrition(id));
    }

    @PostMapping("/calculate")
    public Result<RecipeNutritionDTO> calculateNutrition(@RequestBody Map<String, Object> body) {
        String ingredientsJson = (String) body.get("ingredients");
        if (ingredientsJson == null && body.get("ingredients") instanceof List) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                ingredientsJson = mapper.writeValueAsString(body.get("ingredients"));
            } catch (Exception e) {
                return Result.error("食材数据格式错误");
            }
        }
        if (ingredientsJson == null || ingredientsJson.isEmpty()) {
            return Result.error("食材数据不能为空");
        }
        return Result.success(nutritionService.calculateRecipeNutrition(ingredientsJson));
    }

    @GetMapping("/recipe/{recipeId}")
    public Result<RecipeNutritionDTO> calculateRecipeNutrition(@PathVariable Long recipeId) {
        return Result.success(nutritionService.calculateRecipeNutritionById(recipeId));
    }
}
