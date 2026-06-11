package com.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recipe")
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String coverImage;

    private String author;

    private Integer cookTime;

    private Integer difficulty;

    private Integer favoriteCount;

    private Integer viewCount;

    private String tags;

    private String ingredients;

    private String steps;

    private String tips;

    private Integer status;

    private Integer isDraft;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private LocalDateTime viewTime;
}
