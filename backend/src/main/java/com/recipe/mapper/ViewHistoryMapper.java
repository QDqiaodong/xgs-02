package com.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.entity.Recipe;
import com.recipe.entity.ViewHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ViewHistoryMapper extends BaseMapper<ViewHistory> {

    @Select("SELECT r.*, vh.created_at as view_time FROM view_history vh " +
            "LEFT JOIN recipe r ON vh.recipe_id = r.id " +
            "WHERE vh.user_id = #{userId} AND vh.deleted = 0 AND r.deleted = 0 " +
            "ORDER BY vh.created_at DESC")
    Page<Recipe> selectViewHistoryWithRecipes(Page<Recipe> page, @Param("userId") Long userId);
}
