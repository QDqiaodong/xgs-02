package com.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recipe.entity.ShoppingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShoppingItemMapper extends BaseMapper<ShoppingItem> {

    @Update("UPDATE shopping_item SET purchased = #{purchased}, updated_at = NOW() WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int updatePurchased(@Param("id") Long id, @Param("userId") Long userId, @Param("purchased") Integer purchased);
}
