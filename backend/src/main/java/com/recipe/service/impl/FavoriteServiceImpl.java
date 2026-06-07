package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recipe.entity.Favorite;
import com.recipe.entity.Recipe;
import com.recipe.mapper.FavoriteMapper;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.FavoriteService;
import com.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final RecipeMapper recipeMapper;
    private final RecipeService recipeService;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_INTERVAL_MS = 100;

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
        if (recipeId == null) {
            return false;
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getRecipeId, recipeId)
                .eq(Favorite::getDeleted, 0);

        if (favoriteMapper.selectCount(wrapper) > 0) {
            log.info("收藏操作幂等命中，recipeId={} 已收藏", recipeId);
            return true;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(1L);
        favorite.setRecipeId(recipeId);
        int inserted = favoriteMapper.insert(favorite);
        if (inserted <= 0) {
            log.warn("插入收藏记录失败，recipeId={}", recipeId);
            return false;
        }

        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null) {
            recipe.setFavoriteCount(recipe.getFavoriteCount() == null ? 1 : recipe.getFavoriteCount() + 1);
            recipeMapper.updateById(recipe);
        }

        updateCacheWithRetry(recipeId, 1);

        return true;
    }

    @Override
    @Transactional
    public boolean removeFavorite(Long recipeId) {
        if (recipeId == null) {
            return false;
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getRecipeId, recipeId)
                .eq(Favorite::getDeleted, 0);

        if (favoriteMapper.selectCount(wrapper) == 0) {
            log.info("取消收藏操作幂等命中，recipeId={} 未收藏", recipeId);
            return true;
        }

        int deleted = favoriteMapper.delete(wrapper);
        if (deleted <= 0) {
            log.warn("删除收藏记录失败，recipeId={}", recipeId);
            return false;
        }

        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null && recipe.getFavoriteCount() != null && recipe.getFavoriteCount() > 0) {
            recipe.setFavoriteCount(recipe.getFavoriteCount() - 1);
            recipeMapper.updateById(recipe);
        }

        updateCacheWithRetry(recipeId, -1);

        return true;
    }

    @Override
    @Transactional
    public boolean removeFavoritesBatch(List<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return true;
        }
        for (Long recipeId : recipeIds) {
            removeFavorite(recipeId);
        }
        return true;
    }

    private void updateCacheWithRetry(Long recipeId, Integer delta) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                recipeService.updateHotRecipeCache(recipeId, delta);
                log.debug("热门榜单缓存更新成功，recipeId={}, delta={}, attempt={}", recipeId, delta, attempt);
                return;
            } catch (Exception e) {
                log.warn("热门榜单缓存更新失败，recipeId={}, delta={}, attempt={}/{}", recipeId, delta, attempt, MAX_RETRY, e);
                if (attempt < MAX_RETRY) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("热门榜单缓存更新最终失败，recipeId={}, delta={}", recipeId, delta, e);
                }
            }
        }
    }
}