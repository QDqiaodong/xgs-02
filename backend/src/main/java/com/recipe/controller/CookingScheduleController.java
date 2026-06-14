package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.dto.CookingScheduleDTO;
import com.recipe.service.CookingScheduleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/cooking-schedule")
@RequiredArgsConstructor
public class CookingScheduleController {

    private final CookingScheduleService cookingScheduleService;

    @GetMapping("/recipe/{recipeId}")
    public Result<CookingScheduleDTO> generateSchedule(
            @PathVariable Long recipeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime mealTime) {
        CookingScheduleDTO schedule = cookingScheduleService.generateSchedule(recipeId, mealTime);
        return Result.success(schedule);
    }

    @PostMapping("/custom")
    public Result<CookingScheduleDTO> generateCustomSchedule(
            @RequestBody ScheduleRequest request) {
        CookingScheduleDTO schedule = cookingScheduleService.generateScheduleFromSteps(
                request.getRecipeTitle(),
                request.getStepsJson(),
                request.getMealTime()
        );
        return Result.success(schedule);
    }

    @Data
    public static class ScheduleRequest {
        private String recipeTitle;
        private String stepsJson;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime mealTime;
    }
}
