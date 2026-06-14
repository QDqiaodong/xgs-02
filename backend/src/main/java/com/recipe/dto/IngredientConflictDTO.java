package com.recipe.dto;

import lombok.Data;
import java.util.List;

@Data
public class IngredientConflictDTO {

    private Boolean hasConflict;

    private String riskLevel;

    private List<ConflictItem> conflicts;

    private List<String> suggestions;

    @Data
    public static class ConflictItem {
        private String ingredientName;

        private String conflictType;

        private String riskLevel;

        private String reason;

        private String replacementSuggestion;

        private String severity;
    }
}
