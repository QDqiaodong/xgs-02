package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.entity.Recipe;
import com.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final RecipeService recipeService;

    @GetMapping("/recipes")
    public Result<List<Recipe>> getUserRecipes() {
        return Result.success(recipeService.getUserRecipes());
    }
}