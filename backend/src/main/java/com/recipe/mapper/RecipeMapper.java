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

    @Select("SELECT r.*, COUNT(f.id) as favorite_count FROM recipe r " +
            "LEFT JOIN favorite f ON r.id = f.recipe_id " +
            "WHERE r.status = 1 AND r.is_draft = 0 AND r.deleted = 0 " +
            "AND f.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY r.id " +
            "ORDER BY favorite_count DESC " +
            "LIMIT #{limit}")
    List<Recipe> selectWeeklyHotRecipes(@Param("limit") Integer limit);

    @Select("SELECT r.*, COUNT(f.id) as favorite_count FROM recipe r " +
            "LEFT JOIN favorite f ON r.id = f.recipe_id " +
            "WHERE r.status = 1 AND r.is_draft = 0 AND r.deleted = 0 " +
            "AND f.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY r.id " +
            "ORDER BY favorite_count DESC " +
            "LIMIT #{limit}")
    List<Recipe> selectMonthlyHotRecipes(@Param("limit") Integer limit);

    @Select("<script>" +
            "SELECT r.* FROM recipe r " +
            "WHERE r.status = 1 AND r.is_draft = 0 AND r.deleted = 0 " +
            "<if test='cuisine != null and cuisine != \"\"'>AND r.tags LIKE CONCAT('%', #{cuisine}, '%')</if> " +
            "<if test='scene != null and scene != \"\"'>AND r.tags LIKE CONCAT('%', #{scene}, '%')</if> " +
            "<if test='difficulty != null'>AND r.difficulty = #{difficulty}</if> " +
            "AND (" +
            "  r.title LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR r.description LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR r.tags LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR JSON_SEARCH(r.ingredients, 'one', CONCAT('%', #{keyword}, '%'), NULL, '$[*].name') IS NOT NULL " +
            ") " +
            "ORDER BY (" +
            "  CASE WHEN r.title LIKE CONCAT('%', #{keyword}, '%') THEN 10 ELSE 0 END + " +
            "  CASE WHEN r.tags LIKE CONCAT('%', #{keyword}, '%') THEN 5 ELSE 0 END + " +
            "  CASE WHEN JSON_SEARCH(r.ingredients, 'one', CONCAT('%', #{keyword}, '%'), NULL, '$[*].name') IS NOT NULL THEN 4 ELSE 0 END + " +
            "  CASE WHEN r.description LIKE CONCAT('%', #{keyword}, '%') THEN 3 ELSE 0 END " +
            ") DESC, r.favorite_count DESC " +
            "</script>")
    List<Recipe> searchRecipesWithWeight(@Param("keyword") String keyword,
                                         @Param("cuisine") String cuisine,
                                         @Param("scene") String scene,
                                         @Param("difficulty") Integer difficulty);
}