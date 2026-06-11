package com.recipe.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.entity.Recipe;

public interface ViewHistoryService {

    Page<Recipe> getViewHistory(int page, int size);

    boolean addViewHistory(Long recipeId);

    boolean removeViewHistory(Long recipeId);

    boolean clearAllHistory();
}
