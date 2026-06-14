package com.recipe.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CookingScheduleDTO {

    private Long recipeId;

    private String recipeTitle;

    private LocalDateTime mealTime;

    private LocalDateTime startTime;

    private Integer totalDuration;

    private List<ScheduleStep> steps;

    private List<ScheduleReminder> reminders;

    @Data
    public static class ScheduleStep {
        private Integer stepIndex;

        private String content;

        private String stepType;

        private Integer duration;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        private Boolean isParallel;

        private String parallelGroup;

        private String description;
    }

    @Data
    public static class ScheduleReminder {
        private String id;

        private String title;

        private String content;

        private LocalDateTime remindTime;

        private Integer advanceMinutes;

        private String type;
    }
}
