package com.recipe.dto;

import com.recipe.entity.Recipe;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecipeDetailDTO extends Recipe {

    private FlavorAdjustmentDTO flavorAdjustment;
}
