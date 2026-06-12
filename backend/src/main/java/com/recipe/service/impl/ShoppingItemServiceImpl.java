package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recipe.entity.ShoppingItem;
import com.recipe.mapper.ShoppingItemMapper;
import com.recipe.service.ShoppingItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingItemServiceImpl implements ShoppingItemService {

    private final ShoppingItemMapper shoppingItemMapper;

    private static final Long DEFAULT_USER_ID = 1L;

    @Override
    public List<ShoppingItem> getShoppingList() {
        LambdaQueryWrapper<ShoppingItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingItem::getUserId, DEFAULT_USER_ID)
                .eq(ShoppingItem::getDeleted, 0)
                .orderByAsc(ShoppingItem::getPurchased)
                .orderByDesc(ShoppingItem::getCreatedAt);
        return shoppingItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public ShoppingItem addShoppingItem(ShoppingItem item) {
        if (item.getIngredientName() == null || item.getIngredientName().trim().isEmpty()) {
            return null;
        }
        item.setUserId(DEFAULT_USER_ID);
        if (item.getPurchased() == null) {
            item.setPurchased(0);
        }
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        shoppingItemMapper.insert(item);
        return item;
    }

    @Override
    @Transactional
    public boolean updateShoppingItem(Long id, ShoppingItem item) {
        if (id == null) {
            return false;
        }
        LambdaQueryWrapper<ShoppingItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingItem::getId, id)
                .eq(ShoppingItem::getUserId, DEFAULT_USER_ID)
                .eq(ShoppingItem::getDeleted, 0);
        ShoppingItem existing = shoppingItemMapper.selectOne(wrapper);
        if (existing == null) {
            return false;
        }
        if (item.getIngredientName() != null) {
            existing.setIngredientName(item.getIngredientName());
        }
        if (item.getAmount() != null) {
            existing.setAmount(item.getAmount());
        }
        if (item.getNote() != null) {
            existing.setNote(item.getNote());
        }
        if (item.getSortOrder() != null) {
            existing.setSortOrder(item.getSortOrder());
        }
        return shoppingItemMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional
    public boolean deleteShoppingItem(Long id) {
        if (id == null) {
            return false;
        }
        LambdaQueryWrapper<ShoppingItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingItem::getId, id)
                .eq(ShoppingItem::getUserId, DEFAULT_USER_ID)
                .eq(ShoppingItem::getDeleted, 0);
        return shoppingItemMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional
    public boolean togglePurchased(Long id, Integer purchased) {
        if (id == null || purchased == null) {
            return false;
        }
        int validPurchased = purchased > 0 ? 1 : 0;
        return shoppingItemMapper.updatePurchased(id, DEFAULT_USER_ID, validPurchased) > 0;
    }

    @Override
    @Transactional
    public boolean addFromRecipe(Long recipeId, List<Map<String, Object>> ingredients) {
        if (recipeId == null || ingredients == null || ingredients.isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<ShoppingItem> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(ShoppingItem::getUserId, DEFAULT_USER_ID)
                .eq(ShoppingItem::getRecipeId, recipeId)
                .eq(ShoppingItem::getDeleted, 0);
        List<ShoppingItem> existingItems = shoppingItemMapper.selectList(existingWrapper);
        Set<String> existingNames = new HashSet<>();
        for (ShoppingItem item : existingItems) {
            if (item.getIngredientName() != null) {
                existingNames.add(item.getIngredientName());
            }
        }

        int count = 0;
        int order = existingItems.size();
        for (Map<String, Object> ing : ingredients) {
            if (ing == null) continue;
            String name = (String) ing.get("name");
            if (name == null || name.trim().isEmpty()) continue;
            name = name.trim();
            if (existingNames.contains(name)) continue;

            ShoppingItem item = new ShoppingItem();
            item.setUserId(DEFAULT_USER_ID);
            item.setRecipeId(recipeId);
            item.setIngredientName(name);
            item.setAmount((String) ing.get("amount"));
            item.setNote((String) ing.get("note"));
            item.setPurchased(0);
            item.setSortOrder(order++);
            shoppingItemMapper.insert(item);
            count++;
            existingNames.add(name);
        }
        log.info("从食谱 {} 加入购物清单，新增 {} 个食材", recipeId, count);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        LambdaQueryWrapper<ShoppingItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingItem::getUserId, DEFAULT_USER_ID)
                .in(ShoppingItem::getId, ids)
                .eq(ShoppingItem::getDeleted, 0);
        return shoppingItemMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional
    public boolean clearPurchased() {
        LambdaQueryWrapper<ShoppingItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingItem::getUserId, DEFAULT_USER_ID)
                .eq(ShoppingItem::getPurchased, 1)
                .eq(ShoppingItem::getDeleted, 0);
        return shoppingItemMapper.delete(wrapper) > 0;
    }
}
