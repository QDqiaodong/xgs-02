package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.entity.ShoppingItem;
import com.recipe.service.ShoppingItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shopping-list")
@RequiredArgsConstructor
public class ShoppingItemController {

    private final ShoppingItemService shoppingItemService;

    @GetMapping
    public Result<List<ShoppingItem>> getShoppingList() {
        return Result.success(shoppingItemService.getShoppingList());
    }

    @PostMapping
    public Result<ShoppingItem> addShoppingItem(@RequestBody ShoppingItem item) {
        ShoppingItem saved = shoppingItemService.addShoppingItem(item);
        if (saved == null) {
            return Result.error("食材名称不能为空");
        }
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateShoppingItem(@PathVariable Long id, @RequestBody ShoppingItem item) {
        return Result.success(shoppingItemService.updateShoppingItem(id, item));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteShoppingItem(@PathVariable Long id) {
        return Result.success(shoppingItemService.deleteShoppingItem(id));
    }

    @PatchMapping("/{id}/purchased")
    public Result<Boolean> togglePurchased(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer purchased = body.get("purchased");
        return Result.success(shoppingItemService.togglePurchased(id, purchased));
    }

    @PostMapping("/from-recipe")
    public Result<Boolean> addFromRecipe(@RequestBody Map<String, Object> body) {
        Long recipeId = Long.valueOf(body.get("recipeId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ingredients = (List<Map<String, Object>>) body.get("ingredients");
        return Result.success(shoppingItemService.addFromRecipe(recipeId, ingredients));
    }

    @DeleteMapping("/batch")
    public Result<Boolean> deleteBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        return Result.success(shoppingItemService.deleteBatch(ids));
    }

    @DeleteMapping("/clear-purchased")
    public Result<Boolean> clearPurchased() {
        return Result.success(shoppingItemService.clearPurchased());
    }
}
