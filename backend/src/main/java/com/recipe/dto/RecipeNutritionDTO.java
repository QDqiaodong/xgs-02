package com.recipe.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RecipeNutritionDTO {

    private BigDecimal totalCalories;

    private BigDecimal totalProtein;

    private BigDecimal totalFat;

    private BigDecimal totalCarbohydrate;

    private BigDecimal totalFiber;

    private BigDecimal totalSodium;

    private List<IngredientNutritionDetail> ingredientDetails;

    private List<String> unmatchedIngredients;

    @Data
    public static class IngredientNutritionDetail {
        private String name;
        private String amount;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal carbohydrate;
        private BigDecimal fiber;
        private BigDecimal sodium;
        private boolean matched;
        private String matchName;
    }
}
