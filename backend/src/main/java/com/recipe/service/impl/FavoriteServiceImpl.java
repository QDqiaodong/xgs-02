package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recipe.entity.Favorite;
import com.recipe.entity.Recipe;
import com.recipe.mapper.FavoriteMapper;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final RecipeMapper recipeMapper;

    @Override
    public List<Recipe> getFavorites() {
        List<Long> recipeIds = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .select(Favorite::getRecipeId)
                        .eq(Favorite::getDeleted, 0)
        ).stream().map(Favorite::getRecipeId).collect(Collectors.toList());

        if (recipeIds.isEmpty()) {
            return List.of();
        }

        return recipeMapper.selectBatchIds(recipeIds);
    }

    @Override
    @Transactional
    public boolean addFavorite(Long recipeId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getRecipeId, recipeId)
                .eq(Favorite::getDeleted, 0);
        
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return true;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(1L);
        favorite.setRecipeId(recipeId);
        favoriteMapper.insert(favorite);

        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null) {
            recipe.setFavoriteCount(recipe.getFavoriteCount() == null ? 1 : recipe.getFavoriteCount() + 1);
            recipeMapper.updateById(recipe);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean removeFavorite(Long recipeId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getRecipeId, recipeId);
        boolean result = favoriteMapper.delete(wrapper) > 0;

        if (result) {
            Recipe recipe = recipeMapper.selectById(recipeId);
            if (recipe != null && recipe.getFavoriteCount() != null && recipe.getFavoriteCount() > 0) {
                recipe.setFavoriteCount(recipe.getFavoriteCount() - 1);
                recipeMapper.updateById(recipe);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public boolean removeFavoritesBatch(List<Long> recipeIds) {
        for (Long recipeId : recipeIds) {
            removeFavorite(recipeId);
        }
        return true;
    }
}