package com.recipe.dto;

import lombok.Data;
import java.util.List;

@Data
public class FlavorAdjustmentDTO {

    private Boolean hasAdjustment;

    private List<SeasoningTip> seasoningTips;

    private List<CookingSuggestion> cookingSuggestions;

    private List<String> summary;

    @Data
    public static class SeasoningTip {
        private String seasoningName;
        private String originalAmount;
        private String suggestedAmount;
        private String adjustType;
        private String reason;
    }

    @Data
    public static class CookingSuggestion {
        private String stepIndex;
        private String originalContent;
        private String suggestedContent;
        private String adjustType;
        private String reason;
    }
}
