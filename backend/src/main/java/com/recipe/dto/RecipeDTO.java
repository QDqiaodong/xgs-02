package com.recipe.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecipeDTO {

    private Long id;

    private String title;

    private String description;

    private String coverImage;

    private String author;

    private Integer cookTime;

    private Integer difficulty;

    private List<String> tags;

    private List<IngredientItem> ingredients;

    private List<StepItem> steps;

    private String tips;

    private Integer isDraft;

    @Data
    public static class IngredientItem {
        private String name;
        private String amount;
        private String note;
    }

    @Data
    public static class StepItem {
        private String content;
        private String image;
    }
}