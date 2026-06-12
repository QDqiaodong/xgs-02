package com.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recipe.entity.IngredientNutrition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IngredientNutritionMapper extends BaseMapper<IngredientNutrition> {

    @Select("SELECT * FROM ingredient_nutrition WHERE deleted = 0 AND name = #{name} LIMIT 1")
    IngredientNutrition selectByName(@Param("name") String name);

    @Select("<script>" +
            "SELECT * FROM ingredient_nutrition " +
            "WHERE deleted = 0 AND name IN " +
            "<foreach item='name' collection='names' open='(' separator=',' close=')'>" +
            "#{name}" +
            "</foreach>" +
            "</script>")
    List<IngredientNutrition> selectByNames(@Param("names") List<String> names);

    @Select("SELECT DISTINCT category FROM ingredient_nutrition WHERE deleted = 0 AND category IS NOT NULL AND category != '' ORDER BY category")
    List<String> selectAllCategories();
}
