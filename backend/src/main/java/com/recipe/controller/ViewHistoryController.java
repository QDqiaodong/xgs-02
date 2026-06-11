package com.recipe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.common.Result;
import com.recipe.entity.Recipe;
import com.recipe.service.ViewHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/view-history")
@RequiredArgsConstructor
public class ViewHistoryController {

    private final ViewHistoryService viewHistoryService;

    @GetMapping
    public Result<Page<Recipe>> getViewHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(viewHistoryService.getViewHistory(page, size));
    }

    @DeleteMapping("/{recipeId}")
    public Result<Boolean> removeViewHistory(@PathVariable Long recipeId) {
        return Result.success(viewHistoryService.removeViewHistory(recipeId));
    }

    @DeleteMapping("/clear")
    public Result<Boolean> clearAllHistory() {
        return Result.success(viewHistoryService.clearAllHistory());
    }
}
