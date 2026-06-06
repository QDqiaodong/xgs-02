package com.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recipe.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {

    @Select("SELECT r.*, COUNT(f.id) as favorite_count FROM recipe r " +
            "LEFT JOIN favorite f ON r.id = f.recipe_id " +
            "WHERE r.status = 1 AND r.is_draft = 0 AND r.deleted = 0 " +
            "GROUP BY r.id " +
            "ORDER BY favorite_count DESC " +
            "LIMIT #{limit}")
    List<Recipe> selectHotRecipes(@Param("limit") Integer limit);
}