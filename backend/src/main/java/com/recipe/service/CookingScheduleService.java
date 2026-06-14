package com.recipe.service;

import com.recipe.dto.CookingScheduleDTO;
import java.time.LocalDateTime;

public interface CookingScheduleService {

    CookingScheduleDTO generateSchedule(Long recipeId, LocalDateTime mealTime);

    CookingScheduleDTO generateScheduleFromSteps(String recipeTitle, String stepsJson, LocalDateTime mealTime);
}
