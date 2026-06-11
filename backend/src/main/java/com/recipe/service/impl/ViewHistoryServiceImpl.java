package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.entity.Recipe;
import com.recipe.entity.ViewHistory;
import com.recipe.mapper.ViewHistoryMapper;
import com.recipe.service.ViewHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewHistoryServiceImpl implements ViewHistoryService {

    private final ViewHistoryMapper viewHistoryMapper;

    private static final Long DEFAULT_USER_ID = 1L;

    @Override
    public Page<Recipe> getViewHistory(int page, int size) {
        Page<Recipe> pageParam = new Page<>(page, size);
        return viewHistoryMapper.selectViewHistoryWithRecipes(pageParam, DEFAULT_USER_ID);
    }

    @Override
    @Transactional
    public boolean addViewHistory(Long recipeId) {
        if (recipeId == null) {
            return false;
        }

        LambdaQueryWrapper<ViewHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ViewHistory::getUserId, DEFAULT_USER_ID)
                .eq(ViewHistory::getRecipeId, recipeId)
                .eq(ViewHistory::getDeleted, 0);

        ViewHistory existing = viewHistoryMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setCreatedAt(LocalDateTime.now());
            viewHistoryMapper.updateById(existing);
            log.info("更新浏览历史时间，userId={}, recipeId={}", DEFAULT_USER_ID, recipeId);
            return true;
        }

        ViewHistory viewHistory = new ViewHistory();
        viewHistory.setUserId(DEFAULT_USER_ID);
        viewHistory.setRecipeId(recipeId);
        int inserted = viewHistoryMapper.insert(viewHistory);
        if (inserted <= 0) {
            log.warn("插入浏览历史失败，userId={}, recipeId={}", DEFAULT_USER_ID, recipeId);
            return false;
        }

        log.info("新增浏览历史，userId={}, recipeId={}", DEFAULT_USER_ID, recipeId);
        return true;
    }

    @Override
    @Transactional
    public boolean removeViewHistory(Long recipeId) {
        if (recipeId == null) {
            return false;
        }

        LambdaQueryWrapper<ViewHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ViewHistory::getUserId, DEFAULT_USER_ID)
                .eq(ViewHistory::getRecipeId, recipeId)
                .eq(ViewHistory::getDeleted, 0);

        ViewHistory existing = viewHistoryMapper.selectOne(wrapper);
        if (existing == null) {
            log.info("删除浏览历史幂等命中，userId={}, recipeId={} 不存在", DEFAULT_USER_ID, recipeId);
            return true;
        }

        int deleted = viewHistoryMapper.delete(wrapper);
        if (deleted <= 0) {
            log.warn("删除浏览历史失败，userId={}, recipeId={}", DEFAULT_USER_ID, recipeId);
            return false;
        }

        log.info("删除浏览历史成功，userId={}, recipeId={}", DEFAULT_USER_ID, recipeId);
        return true;
    }

    @Override
    @Transactional
    public boolean clearAllHistory() {
        LambdaQueryWrapper<ViewHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ViewHistory::getUserId, DEFAULT_USER_ID)
                .eq(ViewHistory::getDeleted, 0);

        int deleted = viewHistoryMapper.delete(wrapper);
        log.info("清空浏览历史，userId={}, 删除数量={}", DEFAULT_USER_ID, deleted);
        return true;
    }
}
