package com.recipe.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.dto.CookingScheduleDTO;
import com.recipe.entity.Recipe;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.CookingScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookingScheduleServiceImpl implements CookingScheduleService {

    private final RecipeMapper recipeMapper;
    private final ObjectMapper objectMapper;

    private static final int PREP_BUFFER_MINUTES = 5;

    @Override
    public CookingScheduleDTO generateSchedule(Long recipeId, LocalDateTime mealTime) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new RuntimeException("菜谱不存在");
        }

        String stepsJson = recipe.getSteps();
        return generateScheduleFromSteps(recipe.getTitle(), stepsJson, mealTime);
    }

    @Override
    public CookingScheduleDTO generateScheduleFromSteps(String recipeTitle, String stepsJson, LocalDateTime mealTime) {
        List<Map<String, Object>> steps = parseSteps(stepsJson);
        if (steps.isEmpty()) {
            throw new RuntimeException("菜谱步骤为空");
        }

        List<CookingScheduleDTO.ScheduleStep> scheduleSteps = calculateStepTiming(steps, mealTime);

        int totalDuration = calculateTotalDuration(scheduleSteps);
        LocalDateTime startTime = scheduleSteps.isEmpty() ? mealTime : scheduleSteps.get(0).getStartTime();

        List<CookingScheduleDTO.ScheduleReminder> reminders = generateReminders(scheduleSteps, mealTime);

        CookingScheduleDTO dto = new CookingScheduleDTO();
        dto.setRecipeTitle(recipeTitle);
        dto.setMealTime(mealTime);
        dto.setStartTime(startTime);
        dto.setTotalDuration(totalDuration);
        dto.setSteps(scheduleSteps);
        dto.setReminders(reminders);

        return dto;
    }

    private List<Map<String, Object>> parseSteps(String stepsJson) {
        if (stepsJson == null || stepsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(stepsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析步骤JSON失败", e);
            return Collections.emptyList();
        }
    }

    private List<CookingScheduleDTO.ScheduleStep> calculateStepTiming(List<Map<String, Object>> steps, LocalDateTime mealTime) {
        List<CookingScheduleDTO.ScheduleStep> scheduleSteps = new ArrayList<>();

        List<StepInfo> stepInfos = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String content = step.get("content") != null ? step.get("content").toString() : "";
            StepInfo stepInfo = analyzeStep(i, content);
            stepInfos.add(stepInfo);
        }

        LocalDateTime currentEndTime = mealTime;

        for (int i = stepInfos.size() - 1; i >= 0; i--) {
            StepInfo stepInfo = stepInfos.get(i);

            CookingScheduleDTO.ScheduleStep scheduleStep = new CookingScheduleDTO.ScheduleStep();
            scheduleStep.setStepIndex(i);
            scheduleStep.setContent(stepInfo.content);
            scheduleStep.setStepType(stepInfo.type);
            scheduleStep.setDuration(stepInfo.duration);
            scheduleStep.setDescription(stepInfo.description);
            scheduleStep.setIsParallel(stepInfo.isParallel);
            scheduleStep.setParallelGroup(stepInfo.parallelGroup);

            LocalDateTime stepEndTime = currentEndTime;
            LocalDateTime stepStartTime = stepEndTime.minusMinutes(stepInfo.duration);

            scheduleStep.setStartTime(stepStartTime);
            scheduleStep.setEndTime(stepEndTime);

            scheduleSteps.add(0, scheduleStep);
            currentEndTime = stepStartTime;
        }

        return scheduleSteps;
    }

    private StepInfo analyzeStep(int index, String content) {
        StepInfo stepInfo = new StepInfo();
        stepInfo.content = content;
        stepInfo.index = index;

        int duration = estimateDuration(content);
        stepInfo.duration = duration;

        String type = classifyStepType(content);
        stepInfo.type = type;

        stepInfo.description = getTypeDescription(type, duration);

        if (isParallelStep(content, type)) {
            stepInfo.isParallel = true;
            stepInfo.parallelGroup = "parallel-" + index;
        } else {
            stepInfo.isParallel = false;
        }

        return stepInfo;
    }

    private int estimateDuration(String content) {
        int minutes = 5;

        Pattern pattern = Pattern.compile("(\\d+)\\s*分钟");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            minutes = Integer.parseInt(matcher.group(1));
            return minutes;
        }

        Pattern secPattern = Pattern.compile("(\\d+)\\s*秒");
        Matcher secMatcher = secPattern.matcher(content);
        if (secMatcher.find()) {
            int seconds = Integer.parseInt(secMatcher.group(1));
            minutes = Math.max(1, (int) Math.ceil(seconds / 60.0));
            return minutes;
        }

        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("炖煮") || lowerContent.contains("慢炖") || lowerContent.contains("卤") || lowerContent.contains("焖")) {
            minutes = 45;
        } else if (lowerContent.contains("蒸")) {
            minutes = 15;
        } else if (lowerContent.contains("腌制") || lowerContent.contains("腌") || lowerContent.contains("浸泡")) {
            minutes = 20;
        } else if (lowerContent.contains("焯水") || lowerContent.contains("汆")) {
            minutes = 5;
        } else if (lowerContent.contains("翻炒") || lowerContent.contains("炒")) {
            minutes = 5;
        } else if (lowerContent.contains("收汁")) {
            minutes = 10;
        } else if (lowerContent.contains("切") || lowerContent.contains("准备") || lowerContent.contains("洗净") || lowerContent.contains("处理")) {
            minutes = 10;
        } else if (lowerContent.contains("调味") || lowerContent.contains("拌匀") || lowerContent.contains("搅拌")) {
            minutes = 3;
        }

        return minutes;
    }

    private String classifyStepType(String content) {
        String lowerContent = content.toLowerCase();

        if (lowerContent.contains("切") || lowerContent.contains("准备") || lowerContent.contains("洗净") ||
            lowerContent.contains("处理") || lowerContent.contains("择") || lowerContent.contains("剥") ||
            lowerContent.contains("去皮") || lowerContent.contains("切丁") || lowerContent.contains("切片") ||
            lowerContent.contains("切丝") || lowerContent.contains("切块")) {
            return "prep";
        }

        if (lowerContent.contains("等待") || lowerContent.contains("腌制") || lowerContent.contains("腌") ||
            lowerContent.contains("浸泡") || lowerContent.contains("醒") || lowerContent.contains("发酵") ||
            lowerContent.contains("静置") || lowerContent.contains("放凉") || lowerContent.contains("冷却")) {
            return "waiting";
        }

        if (lowerContent.contains("炒") || lowerContent.contains("煎") || lowerContent.contains("炸") ||
            lowerContent.contains("烤") || lowerContent.contains("蒸") || lowerContent.contains("煮") ||
            lowerContent.contains("炖") || lowerContent.contains("焖") || lowerContent.contains("卤") ||
            lowerContent.contains("烧") || lowerContent.contains("爆") || lowerContent.contains("煸炒")) {
            return "heating";
        }

        if (lowerContent.contains("调味") || lowerContent.contains("拌匀") || lowerContent.contains("搅拌") ||
            lowerContent.contains("摆盘") || lowerContent.contains("盛出") || lowerContent.contains("出锅")) {
            return "sync";
        }

        return "prep";
    }

    private String getTypeDescription(String type, int duration) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("prep", "备菜阶段");
        descriptions.put("heating", "加热烹饪");
        descriptions.put("waiting", "等待/腌制");
        descriptions.put("sync", "同步操作");
        return descriptions.getOrDefault(type, "其他操作");
    }

    private boolean isParallelStep(String content, String type) {
        String lowerContent = content.toLowerCase();
        if ("waiting".equals(type)) {
            return true;
        }
        if (lowerContent.contains("同时") || lowerContent.contains("一边") || lowerContent.contains("另外")) {
            return true;
        }
        return false;
    }

    private int calculateTotalDuration(List<CookingScheduleDTO.ScheduleStep> steps) {
        if (steps.isEmpty()) {
            return 0;
        }
        LocalDateTime firstStart = steps.get(0).getStartTime();
        LocalDateTime lastEnd = steps.get(steps.size() - 1).getEndTime();
        return (int) java.time.Duration.between(firstStart, lastEnd).toMinutes();
    }

    private List<CookingScheduleDTO.ScheduleReminder> generateReminders(List<CookingScheduleDTO.ScheduleStep> steps, LocalDateTime mealTime) {
        List<CookingScheduleDTO.ScheduleReminder> reminders = new ArrayList<>();

        if (steps.isEmpty()) {
            return reminders;
        }

        CookingScheduleDTO.ScheduleReminder startReminder = new CookingScheduleDTO.ScheduleReminder();
        startReminder.setId("reminder-start");
        startReminder.setTitle("开始烹饪提醒");
        startReminder.setContent("距离开饭还有" + calculateTotalDuration(steps) + "分钟，该开始准备啦！");
        startReminder.setRemindTime(steps.get(0).getStartTime());
        startReminder.setAdvanceMinutes(0);
        startReminder.setType("start");
        reminders.add(startReminder);

        int reminderIndex = 1;
        for (CookingScheduleDTO.ScheduleStep step : steps) {
            if ("heating".equals(step.getStepType()) || "waiting".equals(step.getStepType())) {
                CookingScheduleDTO.ScheduleReminder stepReminder = new CookingScheduleDTO.ScheduleReminder();
                stepReminder.setId("reminder-step-" + step.getStepIndex());
                stepReminder.setTitle("步骤" + (step.getStepIndex() + 1) + "即将完成");
                stepReminder.setContent(truncateContent(step.getContent(), 50));
                stepReminder.setRemindTime(step.getEndTime().minusMinutes(1));
                stepReminder.setAdvanceMinutes(1);
                stepReminder.setType("step");
                reminders.add(stepReminder);
                reminderIndex++;
            }
        }

        CookingScheduleDTO.ScheduleReminder mealReminder = new CookingScheduleDTO.ScheduleReminder();
        mealReminder.setId("reminder-meal");
        mealReminder.setTitle("开饭啦！");
        mealReminder.setContent("美食已就绪，快趁热享用吧！");
        mealReminder.setRemindTime(mealTime);
        mealReminder.setAdvanceMinutes(0);
        mealReminder.setType("meal");
        reminders.add(mealReminder);

        return reminders;
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private static class StepInfo {
        int index;
        String content;
        String type;
        int duration;
        String description;
        boolean isParallel;
        String parallelGroup;
    }
}
