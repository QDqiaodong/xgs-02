package com.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("family_taste_profile")
public class FamilyTasteProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String profileName;

    private Integer lowOil;

    private Integer lowSalt;

    private Integer lowSugar;

    private Integer noSpicy;

    private Integer lightSpicy;

    private Integer mediumSpicy;

    private Integer heavySpicy;

    private String tastePreferences;

    private String dietaryNotes;

    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
