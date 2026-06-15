package com.recipe.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.dto.RecipeOperabilityAssessmentDTO;
import com.recipe.entity.Recipe;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.RecipeOperabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeOperabilityServiceImpl implements RecipeOperabilityService {

    private final RecipeMapper recipeMapper;
    private final ObjectMapper objectMapper;

    private static final String LEVEL_EXCELLENT = "优秀";
    private static final String LEVEL_GOOD = "良好";
    private static final String LEVEL_AVERAGE = "一般";
    private static final String LEVEL_POOR = "待改进";

    private static final String RISK_HIGH = "高风险";
    private static final String RISK_MEDIUM = "中风险";
    private static final String RISK_LOW = "低风险";

    private static final String CATEGORY_STEP = "步骤";
    private static final String CATEGORY_INGREDIENT = "食材";
    private static final String CATEGORY_SAFETY = "安全";
    private static final String CATEGORY_TIME = "时间";
    private static final String CATEGORY_TOOL = "工具";
    private static final String CATEGORY_TECHNIQUE = "技巧";

    private static final Set<String> VAGUE_AMOUNTS = new HashSet<>(Arrays.asList(
            "适量", "少许", "若干", "少量", "一些", "一点", "酌量", "适当"
    ));

    private static final Set<String> COOKING_ACTIONS = new HashSet<>(Arrays.asList(
            "切", "洗", "焯", "汆", "煮", "蒸", "炒", "煎", "炸", "烤", "炖", "焖",
            "卤", "腌", "拌", "烧", "煨", "煲", "涮", "烘", "焙", "焗", "熏", "泡",
            "勾芡", "收汁", "调味", "拌匀", "搅拌", "打散", "剁碎", "切片", "切丝",
            "切丁", "切块", "拍碎", "捣泥", "去皮", "去骨", "去壳", "解冻", "焯水",
            "过油", "上浆", "挂糊", "摆盘", "装盘"
    ));

    private static final Set<String> TIME_KEYWORDS = new HashSet<>(Arrays.asList(
            "分钟", "小时", "秒", "分", "时", "半分钟", "一分钟", "三分钟", "五分钟",
            "十分钟", "十五分钟", "二十分钟", "三十分钟", "一小时", "两小时", "隔夜",
            "过夜", "半天", "一天"
    ));

    private static final Set<String> TEMP_KEYWORDS = new HashSet<>(Arrays.asList(
            "大火", "中火", "小火", "旺火", "微火", "油温", "七成热", "六成热",
            "五成热", "八成热", "滚开", "沸腾", "冒泡", "冒烟", "金黄", "变焦",
            "冰糖色", "枣红色", "上色", "熟透", "断生", "七分熟", "八分熟"
    ));

    private static final Set<String> HIGH_RISK_OPERATIONS = new HashSet<>(Arrays.asList(
            "油炸", "爆炒", "高温", "高压锅", "压力", "热油", "滚油", "溅油",
            "切肉", "切骨", "锋利", "开水", "蒸汽", "烫"
    ));

    private static final Set<String> ALLERGEN_INGREDIENTS = new HashSet<>(Arrays.asList(
            "花生", "核桃", "杏仁", "腰果", "开心果", "芝麻", "海鲜", "鱼", "虾",
            "蟹", "贝类", "牛奶", "鸡蛋", "大豆", "小麦", "芒果", "菠萝", "猕猴桃"
    ));

    @Override
    public RecipeOperabilityAssessmentDTO assessRecipe(Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new RuntimeException("菜谱不存在");
        }
        return assessRecipeByContent(recipe.getIngredients(), recipe.getSteps(), recipe.getCookTime());
    }

    @Override
    public RecipeOperabilityAssessmentDTO assessRecipeByContent(String ingredientsJson, String stepsJson, Integer cookTime) {
        RecipeOperabilityAssessmentDTO result = new RecipeOperabilityAssessmentDTO();

        List<Map<String, Object>> ingredients = parseIngredients(ingredientsJson);
        List<Map<String, Object>> steps = parseSteps(stepsJson);

        RecipeOperabilityAssessmentDTO.StepClarityAssessment stepClarity = assessStepClarity(steps, cookTime);
        RecipeOperabilityAssessmentDTO.IngredientCompletenessAssessment ingredientCompleteness = assessIngredientCompleteness(ingredients);
        List<RecipeOperabilityAssessmentDTO.RiskWarning> riskWarnings = generateRiskWarnings(ingredients, steps, cookTime);
        List<RecipeOperabilityAssessmentDTO.OptimizationSuggestion> optimizationSuggestions = generateOptimizationSuggestions(
                stepClarity, ingredientCompleteness, riskWarnings, ingredients, steps, cookTime);

        result.setStepClarity(stepClarity);
        result.setIngredientCompleteness(ingredientCompleteness);
        result.setRiskWarnings(riskWarnings);
        result.setOptimizationSuggestions(optimizationSuggestions);

        int overallScore = calculateOverallScore(stepClarity.getScore(), ingredientCompleteness.getScore(), riskWarnings);
        result.setOverallScore(overallScore);
        result.setOverallLevel(getLevelByScore(overallScore));

        return result;
    }

    private RecipeOperabilityAssessmentDTO.StepClarityAssessment assessStepClarity(List<Map<String, Object>> steps, Integer cookTime) {
        RecipeOperabilityAssessmentDTO.StepClarityAssessment assessment = new RecipeOperabilityAssessmentDTO.StepClarityAssessment();
        List<String> issues = new ArrayList<>();
        List<String> highlights = new ArrayList<>();

        int totalSteps = steps.size();
        assessment.setTotalStepCount(totalSteps);

        if (totalSteps == 0) {
            assessment.setScore(0);
            assessment.setLevel(LEVEL_POOR);
            issues.add("菜谱缺少烹饪步骤");
            assessment.setIssues(issues);
            assessment.setHighlights(highlights);
            assessment.setClearStepCount(0);
            assessment.setHasTimeDescription(false);
            assessment.setHasTemperatureDescription(false);
            return assessment;
        }

        int clearSteps = 0;
        boolean hasAnyTimeDesc = false;
        boolean hasAnyTempDesc = false;
        Set<String> usedActions = new HashSet<>();

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String content = getStringValue(step, "content");
            int stepNum = i + 1;

            if (content == null || content.trim().isEmpty()) {
                issues.add("步骤" + stepNum + "内容为空");
                continue;
            }

            boolean stepClear = true;

            if (content.length() < 10) {
                issues.add("步骤" + stepNum + "描述过于简短，可能不够详细");
                stepClear = false;
            }

            boolean stepHasAction = false;
            for (String action : COOKING_ACTIONS) {
                if (content.contains(action)) {
                    stepHasAction = true;
                    usedActions.add(action);
                    break;
                }
            }
            if (!stepHasAction) {
                issues.add("步骤" + stepNum + "未明确说明烹饪动作（如炒、煮、切等）");
                stepClear = false;
            }

            boolean stepHasTime = false;
            for (String timeKw : TIME_KEYWORDS) {
                if (content.contains(timeKw)) {
                    stepHasTime = true;
                    hasAnyTimeDesc = true;
                    break;
                }
            }
            if (!stepHasTime && needsTimeDescription(content)) {
                issues.add("步骤" + stepNum + "缺少时间描述，建议补充烹饪时长");
                stepClear = false;
            }

            boolean stepHasTemp = false;
            for (String tempKw : TEMP_KEYWORDS) {
                if (content.contains(tempKw)) {
                    stepHasTemp = true;
                    hasAnyTempDesc = true;
                    break;
                }
            }
            if (!stepHasTemp && needsTempDescription(content)) {
                issues.add("步骤" + stepNum + "缺少火候/温度描述，建议补充火力说明");
                stepClear = false;
            }

            boolean hasImage = step.get("image") != null && !getStringValue(step, "image").trim().isEmpty();
            if (hasImage) {
                highlights.add("步骤" + stepNum + "配有图片，更加直观");
            }

            if (stepClear) {
                clearSteps++;
            }
        }

        assessment.setClearStepCount(clearSteps);
        assessment.setHasTimeDescription(hasAnyTimeDesc);
        assessment.setHasTemperatureDescription(hasAnyTempDesc);

        if (cookTime != null && cookTime > 0) {
            highlights.add("标注了总烹饪时间：" + cookTime + "分钟");
        } else {
            issues.add("未标注总烹饪时间");
        }

        if (hasAnyTimeDesc) {
            highlights.add("步骤中包含了时间描述");
        }
        if (hasAnyTempDesc) {
            highlights.add("步骤中包含了火候/温度说明");
        }

        if (usedActions.size() >= 3) {
            highlights.add("使用了多种烹饪技法：" + String.join("、", new ArrayList<>(usedActions).subList(0, Math.min(3, usedActions.size()))));
        }

        int baseScore = (int) ((clearSteps * 1.0 / totalSteps) * 70);
        int bonusScore = 0;
        if (hasAnyTimeDesc) bonusScore += 10;
        if (hasAnyTempDesc) bonusScore += 10;
        if (cookTime != null && cookTime > 0) bonusScore += 10;

        int score = Math.min(100, baseScore + bonusScore);
        assessment.setScore(score);
        assessment.setLevel(getLevelByScore(score));
        assessment.setIssues(issues);
        assessment.setHighlights(highlights);

        return assessment;
    }

    private RecipeOperabilityAssessmentDTO.IngredientCompletenessAssessment assessIngredientCompleteness(List<Map<String, Object>> ingredients) {
        RecipeOperabilityAssessmentDTO.IngredientCompletenessAssessment assessment = new RecipeOperabilityAssessmentDTO.IngredientCompletenessAssessment();
        List<String> issues = new ArrayList<>();
        List<String> highlights = new ArrayList<>();
        List<String> vagueIngredients = new ArrayList<>();

        int totalCount = ingredients.size();
        assessment.setTotalCount(totalCount);

        if (totalCount == 0) {
            assessment.setScore(0);
            assessment.setLevel(LEVEL_POOR);
            issues.add("菜谱缺少食材清单");
            assessment.setIssues(issues);
            assessment.setHighlights(highlights);
            assessment.setCompleteCount(0);
            assessment.setVagueIngredients(vagueIngredients);
            return assessment;
        }

        int completeCount = 0;
        boolean hasSeasoning = false;
        boolean hasMainIngredient = false;

        for (int i = 0; i < ingredients.size(); i++) {
            Map<String, Object> ing = ingredients.get(i);
            String name = getStringValue(ing, "name");
            String amount = getStringValue(ing, "amount");

            if (name == null || name.trim().isEmpty()) {
                issues.add("第" + (i + 1) + "个食材缺少名称");
                continue;
            }

            boolean ingredientComplete = true;

            if (amount == null || amount.trim().isEmpty()) {
                issues.add("食材「" + name + "」缺少用量说明");
                ingredientComplete = false;
            } else {
                boolean isVague = false;
                for (String vague : VAGUE_AMOUNTS) {
                    if (amount.contains(vague)) {
                        isVague = true;
                        vagueIngredients.add(name);
                        break;
                    }
                }
                if (isVague) {
                    issues.add("食材「" + name + "」用量不够精确（" + amount + "），建议给出具体数值");
                    ingredientComplete = false;
                } else if (!hasNumericAmount(amount)) {
                    issues.add("食材「" + name + "」用量格式不规范，建议使用\"数字+单位\"格式（如：100克）");
                    ingredientComplete = false;
                }
            }

            if (isSeasoning(name)) {
                hasSeasoning = true;
            }
            if (isMainIngredient(name)) {
                hasMainIngredient = true;
            }

            if (ingredientComplete) {
                completeCount++;
            }
        }

        assessment.setCompleteCount(completeCount);
        assessment.setVagueIngredients(vagueIngredients);

        if (hasSeasoning) {
            highlights.add("包含调味料，口味更完整");
        } else {
            issues.add("食材清单中未包含调味料，可能影响成品口味");
        }

        if (hasMainIngredient) {
            highlights.add("包含主料，食材结构合理");
        }

        if (!vagueIngredients.isEmpty()) {
            highlights.add("建议精确化以下食材用量：" + String.join("、", vagueIngredients));
        }

        int score = (int) ((completeCount * 1.0 / totalCount) * 100);
        if (!hasSeasoning) score = Math.max(0, score - 10);

        assessment.setScore(Math.max(0, Math.min(100, score)));
        assessment.setLevel(getLevelByScore(score));
        assessment.setIssues(issues);
        assessment.setHighlights(highlights);

        return assessment;
    }

    private List<RecipeOperabilityAssessmentDTO.RiskWarning> generateRiskWarnings(
            List<Map<String, Object>> ingredients, List<Map<String, Object>> steps, Integer cookTime) {
        List<RecipeOperabilityAssessmentDTO.RiskWarning> warnings = new ArrayList<>();

        Set<String> ingredientNames = new HashSet<>();
        for (Map<String, Object> ing : ingredients) {
            String name = getStringValue(ing, "name");
            if (name != null) {
                ingredientNames.add(name);
            }
        }

        Set<String> foundAllergens = new HashSet<>();
        for (String ingredient : ingredientNames) {
            for (String allergen : ALLERGEN_INGREDIENTS) {
                if (ingredient.contains(allergen)) {
                    foundAllergens.add(allergen);
                    break;
                }
            }
        }
        if (!foundAllergens.isEmpty()) {
            RecipeOperabilityAssessmentDTO.RiskWarning warning = new RecipeOperabilityAssessmentDTO.RiskWarning();
            warning.setLevel(RISK_MEDIUM);
            warning.setCategory(CATEGORY_SAFETY);
            warning.setDescription("菜谱含有常见过敏原：" + String.join("、", foundAllergens));
            warning.setSuggestion("对上述食材过敏者请谨慎食用，或用其他食材替代");
            warnings.add(warning);
        }

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String content = getStringValue(step, "content");
            int stepNum = i + 1;

            if (content == null) continue;

            for (String highRisk : HIGH_RISK_OPERATIONS) {
                if (content.contains(highRisk)) {
                    RecipeOperabilityAssessmentDTO.RiskWarning warning = new RecipeOperabilityAssessmentDTO.RiskWarning();
                    warning.setLevel(RISK_MEDIUM);
                    warning.setCategory(CATEGORY_SAFETY);
                    warning.setDescription("步骤" + stepNum + "涉及" + highRisk + "操作，需注意安全");
                    warning.setSuggestion(getSafetySuggestion(highRisk));
                    warnings.add(warning);
                    break;
                }
            }
        }

        if (cookTime != null && cookTime > 120) {
            RecipeOperabilityAssessmentDTO.RiskWarning warning = new RecipeOperabilityAssessmentDTO.RiskWarning();
            warning.setLevel(RISK_LOW);
            warning.setCategory(CATEGORY_TIME);
            warning.setDescription("烹饪时间较长（" + cookTime + "分钟），需要提前规划时间");
            warning.setSuggestion("建议分阶段准备，利用炖煮间隙处理其他食材");
            warnings.add(warning);
        }

        boolean hasRawEgg = false;
        boolean hasRawMeat = false;
        for (Map<String, Object> step : steps) {
            String content = getStringValue(step, "content");
            if (content == null) continue;
            if ((content.contains("鸡蛋") || content.contains("蛋液")) &&
                    (content.contains("不煮") || content.contains("生吃") || content.contains("半熟"))) {
                hasRawEgg = true;
            }
            if (content.contains("生鱼片") || (content.contains("肉") && content.contains("生吃"))) {
                hasRawMeat = true;
            }
        }
        if (hasRawEgg) {
            RecipeOperabilityAssessmentDTO.RiskWarning warning = new RecipeOperabilityAssessmentDTO.RiskWarning();
            warning.setLevel(RISK_HIGH);
            warning.setCategory(CATEGORY_SAFETY);
            warning.setDescription("菜谱涉及生/半生鸡蛋，存在沙门氏菌风险");
            warning.setSuggestion("建议选择可生食鸡蛋，或彻底煮熟后食用；老人、小孩、孕妇需特别注意");
            warnings.add(warning);
        }
        if (hasRawMeat) {
            RecipeOperabilityAssessmentDTO.RiskWarning warning = new RecipeOperabilityAssessmentDTO.RiskWarning();
            warning.setLevel(RISK_HIGH);
            warning.setCategory(CATEGORY_SAFETY);
            warning.setDescription("菜谱涉及生食肉类/海鲜，存在微生物感染风险");
            warning.setSuggestion("请确保食材来源可靠、新鲜卫生，建议完全煮熟；免疫力弱者避免生食");
            warnings.add(warning);
        }

        return warnings;
    }

    private List<RecipeOperabilityAssessmentDTO.OptimizationSuggestion> generateOptimizationSuggestions(
            RecipeOperabilityAssessmentDTO.StepClarityAssessment stepClarity,
            RecipeOperabilityAssessmentDTO.IngredientCompletenessAssessment ingredientCompleteness,
            List<RecipeOperabilityAssessmentDTO.RiskWarning> riskWarnings,
            List<Map<String, Object>> ingredients,
            List<Map<String, Object>> steps,
            Integer cookTime) {
        List<RecipeOperabilityAssessmentDTO.OptimizationSuggestion> suggestions = new ArrayList<>();

        Set<String> addedCategories = new HashSet<>();

        if (stepClarity.getScore() < 80) {
            if (!stepClarity.getHasTimeDescription()) {
                RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
                suggestion.setCategory(CATEGORY_TIME);
                suggestion.setPriority("高");
                suggestion.setDescription("为每个步骤添加具体的时间描述，如\"翻炒3分钟\"、\"炖煮20分钟\"等，让烹饪更可控");
                suggestions.add(suggestion);
                addedCategories.add(CATEGORY_TIME);
            }
            if (!stepClarity.getHasTemperatureDescription()) {
                RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
                suggestion.setCategory(CATEGORY_TECHNIQUE);
                suggestion.setPriority("高");
                suggestion.setDescription("补充火候说明，如\"大火烧开转小火\"、\"油温六成热下锅\"等，保证菜品质量");
                suggestions.add(suggestion);
                addedCategories.add(CATEGORY_TECHNIQUE);
            }
        }

        if (ingredientCompleteness.getScore() < 80 && !ingredientCompleteness.getVagueIngredients().isEmpty()) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_INGREDIENT);
            suggestion.setPriority("高");
            suggestion.setDescription("精确化食材用量：将\"适量\"、\"少许\"替换为具体数值（如5克盐、10毫升生抽），便于复刻");
            suggestions.add(suggestion);
            addedCategories.add(CATEGORY_INGREDIENT);
        }

        boolean hasHighRisk = riskWarnings.stream().anyMatch(w -> RISK_HIGH.equals(w.getLevel()));
        boolean hasMediumRisk = riskWarnings.stream().anyMatch(w -> RISK_MEDIUM.equals(w.getLevel()));
        if (hasHighRisk || hasMediumRisk) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_SAFETY);
            suggestion.setPriority(hasHighRisk ? "高" : "中");
            suggestion.setDescription("在菜谱开头添加\"温馨提示\"或\"注意事项\"，提醒特殊人群和操作安全要点");
            suggestions.add(suggestion);
            addedCategories.add(CATEGORY_SAFETY);
        }

        boolean hasImages = steps.stream()
                .anyMatch(s -> getStringValue(s, "image") != null && !getStringValue(s, "image").trim().isEmpty());
        if (!hasImages && steps.size() > 2) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_STEP);
            suggestion.setPriority("中");
            suggestion.setDescription("为关键步骤添加图片，如切配样式、成品颜色、火候状态等，降低新手操作难度");
            suggestions.add(suggestion);
            addedCategories.add(CATEGORY_STEP);
        }

        long stepsWithoutImage = steps.stream()
                .filter(s -> getStringValue(s, "image") == null || getStringValue(s, "image").trim().isEmpty())
                .count();
        if (stepsWithoutImage > 0 && stepsWithoutImage < steps.size()) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_STEP);
            suggestion.setPriority("低");
            suggestion.setDescription("仍有" + stepsWithoutImage + "个步骤缺少配图，可考虑补充完整以提升体验");
            suggestions.add(suggestion);
        }

        List<String> stepContents = new ArrayList<>();
        for (Map<String, Object> step : steps) {
            String content = getStringValue(step, "content");
            if (content != null && content.length() > 150) {
                stepContents.add(content);
            }
        }
        if (!stepContents.isEmpty()) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_STEP);
            suggestion.setPriority("低");
            suggestion.setDescription("有" + stepContents.size() + "个步骤描述过长，建议拆分为多个子步骤，方便逐步操作");
            suggestions.add(suggestion);
        }

        if (cookTime != null && cookTime >= 60 && cookTime < 120) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_TIME);
            suggestion.setPriority("中");
            suggestion.setDescription("建议在步骤中标注哪些操作可以并行进行（如炖煮时准备其他食材），提高效率");
            suggestions.add(suggestion);
        }

        boolean hasNote = steps.stream()
                .anyMatch(s -> {
                    String content = getStringValue(s, "note");
                    return content != null && !content.trim().isEmpty();
                });
        if (!hasNote && steps.size() > 3) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_TECHNIQUE);
            suggestion.setPriority("中");
            suggestion.setDescription("添加\"小贴士\"或\"注意事项\"板块，分享成功关键、常见失败原因和替代方案");
            suggestions.add(suggestion);
        }

        int ingredientCount = ingredients.size();
        if (ingredientCount >= 15) {
            RecipeOperabilityAssessmentDTO.OptimizationSuggestion suggestion = new RecipeOperabilityAssessmentDTO.OptimizationSuggestion();
            suggestion.setCategory(CATEGORY_INGREDIENT);
            suggestion.setPriority("低");
            suggestion.setDescription("食材种类较多（" + ingredientCount + "种），可考虑简化配方或标注\"可选食材\"，降低门槛");
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    private int calculateOverallScore(int stepScore, int ingredientScore, List<RecipeOperabilityAssessmentDTO.RiskWarning> riskWarnings) {
        double weightedScore = stepScore * 0.4 + ingredientScore * 0.4;

        int riskPenalty = 0;
        for (RecipeOperabilityAssessmentDTO.RiskWarning warning : riskWarnings) {
            switch (warning.getLevel()) {
                case RISK_HIGH:
                    riskPenalty += 8;
                    break;
                case RISK_MEDIUM:
                    riskPenalty += 4;
                    break;
                case RISK_LOW:
                    riskPenalty += 1;
                    break;
            }
        }
        riskPenalty = Math.min(riskPenalty, 20);

        int finalScore = (int) (weightedScore * 0.8 + (100 - riskPenalty) * 0.2);
        return Math.max(0, Math.min(100, finalScore));
    }

    private String getLevelByScore(int score) {
        if (score >= 85) return LEVEL_EXCELLENT;
        if (score >= 70) return LEVEL_GOOD;
        if (score >= 50) return LEVEL_AVERAGE;
        return LEVEL_POOR;
    }

    private boolean needsTimeDescription(String content) {
        return content.contains("煮") || content.contains("炖") || content.contains("蒸") ||
                content.contains("炒") || content.contains("煎") || content.contains("炸") ||
                content.contains("烤") || content.contains("焖") || content.contains("卤") ||
                content.contains("腌") || content.contains("泡");
    }

    private boolean needsTempDescription(String content) {
        return content.contains("炒") || content.contains("煎") || content.contains("炸") ||
                content.contains("烤") || content.contains("烧") || content.contains("油温");
    }

    private boolean hasNumericAmount(String amount) {
        Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher matcher = pattern.matcher(amount);
        return matcher.find();
    }

    private boolean isSeasoning(String name) {
        String lower = name.toLowerCase();
        return lower.contains("盐") || lower.contains("糖") || lower.contains("酱油") ||
                lower.contains("生抽") || lower.contains("老抽") || lower.contains("醋") ||
                lower.contains("料酒") || lower.contains("油") || lower.contains("酱") ||
                lower.contains("味精") || lower.contains("鸡精") || lower.contains("胡椒") ||
                lower.contains("八角") || lower.contains("桂皮") || lower.contains("香叶") ||
                lower.contains("辣椒") || lower.contains("花椒") || lower.contains("葱") ||
                lower.contains("姜") || lower.contains("蒜") || lower.contains("芝麻");
    }

    private boolean isMainIngredient(String name) {
        String lower = name.toLowerCase();
        return lower.contains("肉") || lower.contains("鱼") || lower.contains("虾") ||
                lower.contains("蛋") || lower.contains("鸡") || lower.contains("猪") ||
                lower.contains("牛") || lower.contains("羊") || lower.contains("豆腐") ||
                lower.contains("米") || lower.contains("面") || lower.contains("菜") ||
                lower.contains("菇") || lower.contains("蟹") || lower.contains("贝") ||
                lower.contains("排骨") || lower.contains("翅") || lower.contains("爪");
    }

    private String getSafetySuggestion(String operation) {
        switch (operation) {
            case "油炸":
            case "爆炒":
            case "热油":
            case "滚油":
            case "溅油":
                return "操作时佩戴围裙，保持食材表面干燥防溅油，油温不过高，远离锅边防烫伤";
            case "高温":
                return "注意烤箱/空气炸锅高温，使用隔热手套取放，避免烫伤";
            case "高压锅":
            case "压力":
                return "严格按照高压锅使用说明操作，确认放气完成后再开盖，切勿强行开盖";
            case "切肉":
            case "切骨":
            case "锋利":
                return "使用锋利刀具时注意手法，手指卷曲呈猫爪状，避免切伤";
            case "开水":
            case "蒸汽":
            case "烫":
                return "处理沸水和蒸汽时做好防护，缓慢开盖，避免蒸汽扑面烫伤";
            default:
                return "操作时请注意安全，做好防护措施";
        }
    }

    private List<Map<String, Object>> parseIngredients(String ingredientsJson) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (ingredientsJson == null || ingredientsJson.isEmpty()) {
            return result;
        }
        try {
            result = objectMapper.readValue(ingredientsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析食材JSON失败", e);
        }
        return result;
    }

    private List<Map<String, Object>> parseSteps(String stepsJson) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (stepsJson == null || stepsJson.isEmpty()) {
            return result;
        }
        try {
            result = objectMapper.readValue(stepsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析步骤JSON失败", e);
        }
        return result;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object val = map.get(key);
        return val == null ? null : val.toString();
    }
}
