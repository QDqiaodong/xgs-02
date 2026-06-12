package com.recipe.service;

import com.recipe.entity.ShoppingItem;

import java.util.List;
import java.util.Map;

public interface ShoppingItemService {

    List<ShoppingItem> getShoppingList();

    ShoppingItem addShoppingItem(ShoppingItem item);

    boolean updateShoppingItem(Long id, ShoppingItem item);

    boolean deleteShoppingItem(Long id);

    boolean togglePurchased(Long id, Integer purchased);

    boolean addFromRecipe(Long recipeId, List<Map<String, Object>> ingredients);

    boolean deleteBatch(List<Long> ids);

    boolean clearPurchased();
}
