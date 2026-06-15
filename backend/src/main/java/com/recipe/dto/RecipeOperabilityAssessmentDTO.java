package com.recipe.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecipeOperabilityAssessmentDTO {

    private StepClarityAssessment stepClarity;

    private IngredientCompletenessAssessment ingredientCompleteness;

    private List<RiskWarning> riskWarnings;

    private List<OptimizationSuggestion> optimizationSuggestions;

    private Integer overallScore;

    private String overallLevel;

    @Data
    public static class StepClarityAssessment {
        private Integer score;
        private String level;
        private List<String> issues;
        private List<String> highlights;
        private Integer clearStepCount;
        private Integer totalStepCount;
        private Boolean hasTimeDescription;
        private Boolean hasTemperatureDescription;
    }

    @Data
    public static class IngredientCompletenessAssessment {
        private Integer score;
        private String level;
        private List<String> issues;
        private List<String> highlights;
        private Integer completeCount;
        private Integer totalCount;
        private List<String> vagueIngredients;
    }

    @Data
    public static class RiskWarning {
        private String level;
        private String category;
        private String description;
        private String suggestion;
    }

    @Data
    public static class OptimizationSuggestion {
        private String category;
        private String description;
        private String priority;
    }
}
