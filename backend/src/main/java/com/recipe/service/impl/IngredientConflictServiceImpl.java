package com.recipe.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.dto.IngredientConflictDTO;
import com.recipe.dto.UserPreferenceDTO;
import com.recipe.entity.Recipe;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.IngredientConflictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientConflictServiceImpl implements IngredientConflictService {

    private final RecipeMapper recipeMapper;
    private final ObjectMapper objectMapper;

    private static final String RISK_LEVEL_HIGH = "high";
    private static final String RISK_LEVEL_MEDIUM = "medium";
    private static final String RISK_LEVEL_LOW = "low";

    private static final String CONFLICT_TYPE_ALLERGY = "allergy";
    private static final String CONFLICT_TYPE_DIETARY = "dietary";
    private static final String CONFLICT_TYPE_DISLIKE = "dislike";

    private static final Map<String, List<String>> INGREDIENT_CATEGORIES = new HashMap<>();
    private static final Map<String, String> REPLACEMENTS = new HashMap<>();

    static {
        INGREDIENT_CATEGORIES.put("seafood", Arrays.asList("鱼", "虾", "蟹", "贝类", "海鲜", "鱿鱼", "章鱼", "墨鱼", "带鱼", "草鱼", "鲈鱼", "三文鱼", "金枪鱼", "鳕鱼", "鳝鱼", "泥鳅", "黄鳝", "蛤蜊", "蛏子", "生蚝", "扇贝", "鲍鱼", "海参", "海蜇", "海带", "紫菜"));
        INGREDIENT_CATEGORIES.put("meat", Arrays.asList("猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉", "鹅肉", "五花肉", "排骨", "里脊", "鸡腿", "鸡胸", "鸡翅", "鸡爪", "猪蹄", "猪肝", "猪心", "猪肚", "牛肉片", "牛排", "羊排", "羊肉串", "腊肉", "火腿", "香肠", "培根"));
        INGREDIENT_CATEGORIES.put("egg", Arrays.asList("鸡蛋", "鸭蛋", "鹌鹑蛋", "蛋清", "蛋黄", "蛋液"));
        INGREDIENT_CATEGORIES.put("milk", Arrays.asList("牛奶", "酸奶", "奶酪", "芝士", "黄油", "奶油", "炼乳", "奶粉"));
        INGREDIENT_CATEGORIES.put("nut", Arrays.asList("花生", "核桃", "杏仁", "腰果", "开心果", "夏威夷果", "榛子", "松子", "板栗", "瓜子", "芝麻", "花生酱", "芝麻酱"));
        INGREDIENT_CATEGORIES.put("wheat", Arrays.asList("面粉", "小麦", "面条", "面包", "馒头", "饺子皮", "馄饨皮", "包子", "饼干", "蛋糕", "意面", "通心粉"));
        INGREDIENT_CATEGORIES.put("soy", Arrays.asList("大豆", "黄豆", "豆腐", "豆浆", "豆皮", "腐竹", "豆干", "酱油", "生抽", "老抽", "味极鲜", "豆瓣酱", "豆豉"));
        INGREDIENT_CATEGORIES.put("spicy", Arrays.asList("辣椒", "花椒", "麻辣", "剁椒", "小米辣", "干辣椒", "辣椒粉", "辣椒油", "火锅底料", "麻辣香锅", "藤椒", "青花椒"));
        INGREDIENT_CATEGORIES.put("onion", Arrays.asList("葱", "洋葱", "大葱", "小葱", "葱花", "蒜苗", "韭黄", "韭菜"));
        INGREDIENT_CATEGORIES.put("garlic", Arrays.asList("蒜", "大蒜", "蒜瓣", "蒜末", "蒜泥", "蒜片"));
        INGREDIENT_CATEGORIES.put("ginger", Arrays.asList("姜", "生姜", "姜片", "姜丝", "姜末"));
        INGREDIENT_CATEGORIES.put("celery", Arrays.asList("芹菜", "西芹"));
        INGREDIENT_CATEGORIES.put("carrot", Arrays.asList("胡萝卜", "红萝卜"));
        INGREDIENT_CATEGORIES.put("coriander", Arrays.asList("香菜", "芫荽"));
        INGREDIENT_CATEGORIES.put("mushroom", Arrays.asList("蘑菇", "香菇", "金针菇", "平菇", "杏鲍菇", "茶树菇", "虫草花", "木耳", "银耳"));
        INGREDIENT_CATEGORIES.put("bamboo", Arrays.asList("竹笋", "笋干", "冬笋", "春笋"));

        REPLACEMENTS.put("猪肉", "可用鸡肉、豆腐、杏鲍菇代替");
        REPLACEMENTS.put("牛肉", "可用猪肉、鸡肉、香菇代替");
        REPLACEMENTS.put("羊肉", "可用牛肉、鸡肉代替");
        REPLACEMENTS.put("鸡肉", "可用猪肉、豆腐、杏鲍菇代替");
        REPLACEMENTS.put("鱼肉", "可用鸡肉、豆腐代替");
        REPLACEMENTS.put("虾", "可用鸡肉丁、杏鲍菇丁代替");
        REPLACEMENTS.put("蟹", "可用鸡肉、香菇代替");
        REPLACEMENTS.put("鸡蛋", "可用豆腐、山药泥代替");
        REPLACEMENTS.put("牛奶", "可用豆浆、椰汁代替");
        REPLACEMENTS.put("花生", "可用腰果、杏仁代替（如非坚果过敏），或用芝麻、瓜子仁代替");
        REPLACEMENTS.put("辣椒", "可用甜椒、彩椒代替，增加甜味代替辣味");
        REPLACEMENTS.put("花椒", "可用胡椒、小茴香代替");
        REPLACEMENTS.put("葱", "可用洋葱、蒜苗代替，或用葱花油提味");
        REPLACEMENTS.put("蒜", "可用蒜末油、洋葱代替，或用姜蒜粉代替");
        REPLACEMENTS.put("姜", "可用姜丝油、白胡椒代替，或用陈皮增加风味");
        REPLACEMENTS.put("香菜", "可用芹菜叶、薄荷代替，或用葱花提香");
        REPLACEMENTS.put("芹菜", "可用西芹、芦笋、豆角代替");
        REPLACEMENTS.put("胡萝卜", "可用南瓜、红薯、山药代替");
        REPLACEMENTS.put("豆腐", "可用鸡蛋、鸡肉末、杏鲍菇代替");
        REPLACEMENTS.put("酱油", "可用盐+糖+少许醋代替，或用鱼露（非海鲜过敏）");
        REPLACEMENTS.put("生抽", "可用盐+少许糖代替，或用味极鲜代替（如非大豆过敏）");
        REPLACEMENTS.put("老抽", "可用焦糖色+盐代替，或用红烧酱油代替");
        REPLACEMENTS.put("料酒", "可用黄酒、花雕酒代替，或用姜汁+少许醋代替");
        REPLACEMENTS.put("醋", "可用柠檬汁、酸梅酱代替");
        REPLACEMENTS.put("白糖", "可用冰糖、蜂蜜、木糖醇代替");
        REPLACEMENTS.put("盐", "可用低钠盐、酱油代替，或用其他调味料增加风味");
    }

    @Override
    public IngredientConflictDTO checkConflicts(Long recipeId, UserPreferenceDTO userPreference) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new RuntimeException("菜谱不存在");
        }

        List<String> ingredientNames = extractIngredientNames(recipe.getIngredients());
        return checkConflictsByIngredients(ingredientNames, userPreference);
    }

    @Override
    public IngredientConflictDTO checkConflictsByIngredients(List<String> ingredients, UserPreferenceDTO userPreference) {
        if (ingredients == null || ingredients.isEmpty()) {
            IngredientConflictDTO result = new IngredientConflictDTO();
            result.setHasConflict(false);
            result.setRiskLevel("none");
            result.setConflicts(Collections.emptyList());
            result.setSuggestions(Collections.singletonList("菜谱食材清单为空"));
            return result;
        }

        List<IngredientConflictDTO.ConflictItem> conflicts = new ArrayList<>();

        List<String> allergies = userPreference != null && userPreference.getAllergies() != null
                ? userPreference.getAllergies() : Collections.emptyList();
        List<String> dietaryRestrictions = userPreference != null && userPreference.getDietaryRestrictions() != null
                ? userPreference.getDietaryRestrictions() : Collections.emptyList();
        List<String> dislikes = userPreference != null && userPreference.getDislikes() != null
                ? userPreference.getDislikes() : Collections.emptyList();

        for (String ingredient : ingredients) {
            String ingredientName = extractName(ingredient);

            IngredientConflictDTO.ConflictItem allergyConflict = checkAllergyConflict(ingredientName, allergies);
            if (allergyConflict != null) {
                conflicts.add(allergyConflict);
                continue;
            }

            IngredientConflictDTO.ConflictItem dietaryConflict = checkDietaryConflict(ingredientName, dietaryRestrictions);
            if (dietaryConflict != null) {
                conflicts.add(dietaryConflict);
                continue;
            }

            IngredientConflictDTO.ConflictItem dislikeConflict = checkDislikeConflict(ingredientName, dislikes);
            if (dislikeConflict != null) {
                conflicts.add(dislikeConflict);
            }
        }

        String riskLevel = calculateRiskLevel(conflicts);

        List<String> suggestions = generateSuggestions(conflicts, riskLevel);

        IngredientConflictDTO result = new IngredientConflictDTO();
        result.setHasConflict(!conflicts.isEmpty());
        result.setRiskLevel(riskLevel);
        result.setConflicts(conflicts);
        result.setSuggestions(suggestions);

        return result;
    }

    private List<String> extractIngredientNames(String ingredientsJson) {
        List<String> names = new ArrayList<>();
        if (ingredientsJson == null || ingredientsJson.isEmpty()) {
            return names;
        }

        try {
            List<Map<String, Object>> ingredients = objectMapper.readValue(
                    ingredientsJson, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> ing : ingredients) {
                Object name = ing.get("name");
                if (name != null) {
                    names.add(name.toString());
                }
            }
        } catch (Exception e) {
            log.error("解析食材JSON失败", e);
        }

        return names;
    }

    private String extractName(String ingredient) {
        if (ingredient == null) {
            return "";
        }
        return ingredient.trim();
    }

    private IngredientConflictDTO.ConflictItem checkAllergyConflict(String ingredientName, List<String> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return null;
        }

        for (String allergy : allergies) {
            if (matchesIngredient(ingredientName, allergy)) {
                IngredientConflictDTO.ConflictItem item = new IngredientConflictDTO.ConflictItem();
                item.setIngredientName(ingredientName);
                item.setConflictType(CONFLICT_TYPE_ALLERGY);
                item.setRiskLevel(RISK_LEVEL_HIGH);
                item.setSeverity("严重");
                item.setReason("您对" + allergy + "过敏，" + ingredientName + "可能含有过敏原，食用可能引起过敏反应");
                item.setReplacementSuggestion(getReplacementSuggestion(ingredientName, "allergy"));
                return item;
            }
        }

        return null;
    }

    private IngredientConflictDTO.ConflictItem checkDietaryConflict(String ingredientName, List<String> dietaryRestrictions) {
        if (dietaryRestrictions == null || dietaryRestrictions.isEmpty()) {
            return null;
        }

        for (String restriction : dietaryRestrictions) {
            if (matchesDietaryRestriction(ingredientName, restriction)) {
                IngredientConflictDTO.ConflictItem item = new IngredientConflictDTO.ConflictItem();
                item.setIngredientName(ingredientName);
                item.setConflictType(CONFLICT_TYPE_DIETARY);
                item.setRiskLevel(RISK_LEVEL_MEDIUM);
                item.setSeverity("中等");
                item.setReason("您有" + restriction + "饮食限制，" + ingredientName + "不符合您的饮食要求");
                item.setReplacementSuggestion(getReplacementSuggestion(ingredientName, "dietary"));
                return item;
            }
        }

        return null;
    }

    private IngredientConflictDTO.ConflictItem checkDislikeConflict(String ingredientName, List<String> dislikes) {
        if (dislikes == null || dislikes.isEmpty()) {
            return null;
        }

        for (String dislike : dislikes) {
            if (matchesIngredient(ingredientName, dislike)) {
                IngredientConflictDTO.ConflictItem item = new IngredientConflictDTO.ConflictItem();
                item.setIngredientName(ingredientName);
                item.setConflictType(CONFLICT_TYPE_DISLIKE);
                item.setRiskLevel(RISK_LEVEL_LOW);
                item.setSeverity("轻微");
                item.setReason("您不喜欢" + dislike + "，这道菜含有" + ingredientName);
                item.setReplacementSuggestion(getReplacementSuggestion(ingredientName, "dislike"));
                return item;
            }
        }

        return null;
    }

    private boolean matchesIngredient(String ingredientName, String keyword) {
        if (ingredientName == null || keyword == null) {
            return false;
        }

        String lowerIngredient = ingredientName.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        if (lowerIngredient.contains(lowerKeyword) || lowerKeyword.contains(lowerIngredient)) {
            return true;
        }

        List<String> categoryIngredients = INGREDIENT_CATEGORIES.get(lowerKeyword);
        if (categoryIngredients != null) {
            for (String catIng : categoryIngredients) {
                if (lowerIngredient.contains(catIng) || catIng.contains(lowerIngredient)) {
                    return true;
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : INGREDIENT_CATEGORIES.entrySet()) {
            String category = entry.getKey();
            List<String> ingredients = entry.getValue();

            boolean keywordInCategory = category.equals(lowerKeyword) ||
                    ingredients.stream().anyMatch(ing -> ing.contains(lowerKeyword) || lowerKeyword.contains(ing));

            if (keywordInCategory) {
                for (String ing : ingredients) {
                    if (lowerIngredient.contains(ing) || ing.contains(lowerIngredient)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean matchesDietaryRestriction(String ingredientName, String restriction) {
        if (ingredientName == null || restriction == null) {
            return false;
        }

        String lowerIngredient = ingredientName.toLowerCase();
        String lowerRestriction = restriction.toLowerCase();

        switch (lowerRestriction) {
            case "素食":
            case "vegetarian":
            case "vegan":
                return isMeatOrSeafood(lowerIngredient) || isEgg(lowerIngredient) ||
                        (lowerRestriction.equals("vegan") && isDairy(lowerIngredient));
            case "纯素":
            case "vegan严格":
                return isMeatOrSeafood(lowerIngredient) || isEgg(lowerIngredient) || isDairy(lowerIngredient);
            case "清真":
            case "halal":
                return lowerIngredient.contains("猪肉") || lowerIngredient.contains("猪") ||
                        lowerIngredient.contains("火腿") || lowerIngredient.contains("培根") ||
                        lowerIngredient.contains("香肠");
            case "无麸质":
            case "gluten-free":
            case "无小麦":
                return isWheat(lowerIngredient);
            case "无乳制品":
            case "dairy-free":
                return isDairy(lowerIngredient);
            case "无坚果":
            case "nut-free":
                return isNut(lowerIngredient);
            case "低钠":
            case "低盐":
                return lowerIngredient.contains("盐") || lowerIngredient.contains("酱油") ||
                        lowerIngredient.contains("生抽") || lowerIngredient.contains("老抽") ||
                        lowerIngredient.contains("酱") || lowerIngredient.contains("咸菜") ||
                        lowerIngredient.contains("腌");
            case "低糖":
            case "无糖":
                return lowerIngredient.contains("糖") || lowerIngredient.contains("蜂蜜") ||
                        lowerIngredient.contains("蜜") || lowerIngredient.contains("可乐") ||
                        lowerIngredient.contains("汽水");
            case "低脂":
            case "低油":
                return lowerIngredient.contains("肥肉") || lowerIngredient.contains("五花肉") ||
                        lowerIngredient.contains("油") || lowerIngredient.contains("油炸") ||
                        lowerIngredient.contains("奶油") || lowerIngredient.contains("黄油");
            default:
                return matchesIngredient(ingredientName, restriction);
        }
    }

    private boolean isMeatOrSeafood(String ingredient) {
        List<String> meatAndSeafood = new ArrayList<>();
        meatAndSeafood.addAll(INGREDIENT_CATEGORIES.getOrDefault("meat", Collections.emptyList()));
        meatAndSeafood.addAll(INGREDIENT_CATEGORIES.getOrDefault("seafood", Collections.emptyList()));
        for (String item : meatAndSeafood) {
            if (ingredient.contains(item) || item.contains(ingredient)) {
                return true;
            }
        }
        return ingredient.contains("肉") || ingredient.contains("鱼") || ingredient.contains("虾") ||
                ingredient.contains("蟹") || ingredient.contains("贝");
    }

    private boolean isEgg(String ingredient) {
        List<String> eggs = INGREDIENT_CATEGORIES.getOrDefault("egg", Collections.emptyList());
        for (String item : eggs) {
            if (ingredient.contains(item) || item.contains(ingredient)) {
                return true;
            }
        }
        return ingredient.contains("蛋");
    }

    private boolean isDairy(String ingredient) {
        List<String> dairy = INGREDIENT_CATEGORIES.getOrDefault("milk", Collections.emptyList());
        for (String item : dairy) {
            if (ingredient.contains(item) || item.contains(ingredient)) {
                return true;
            }
        }
        return ingredient.contains("奶") || ingredient.contains("酪") || ingredient.contains("奶油");
    }

    private boolean isNut(String ingredient) {
        List<String> nuts = INGREDIENT_CATEGORIES.getOrDefault("nut", Collections.emptyList());
        for (String item : nuts) {
            if (ingredient.contains(item) || item.contains(ingredient)) {
                return true;
            }
        }
        return ingredient.contains("坚果") || ingredient.contains("花生") || ingredient.contains("核桃") ||
                ingredient.contains("杏仁") || ingredient.contains("腰果");
    }

    private boolean isWheat(String ingredient) {
        List<String> wheat = INGREDIENT_CATEGORIES.getOrDefault("wheat", Collections.emptyList());
        for (String item : wheat) {
            if (ingredient.contains(item) || item.contains(ingredient)) {
                return true;
            }
        }
        return ingredient.contains("小麦") || ingredient.contains("面粉") || ingredient.contains("面筋");
    }

    private String getReplacementSuggestion(String ingredientName, String conflictType) {
        for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
            if (ingredientName.contains(entry.getKey()) || entry.getKey().contains(ingredientName)) {
                return entry.getValue();
            }
        }

        switch (conflictType) {
            case "allergy":
                return "建议更换为不含该过敏原的食材，请咨询专业医生或营养师的意见";
            case "dietary":
                return "建议更换为符合您饮食限制的同类食材";
            case "dislike":
                return "可以尝试用您喜欢的同类食材代替，或减少用量";
            default:
                return "可以根据个人口味选择合适的替代食材";
        }
    }

    private String calculateRiskLevel(List<IngredientConflictDTO.ConflictItem> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return "none";
        }

        boolean hasHigh = conflicts.stream()
                .anyMatch(c -> RISK_LEVEL_HIGH.equals(c.getRiskLevel()));
        if (hasHigh) {
            return RISK_LEVEL_HIGH;
        }

        boolean hasMedium = conflicts.stream()
                .anyMatch(c -> RISK_LEVEL_MEDIUM.equals(c.getRiskLevel()));
        if (hasMedium) {
            return RISK_LEVEL_MEDIUM;
        }

        return RISK_LEVEL_LOW;
    }

    private List<String> generateSuggestions(List<IngredientConflictDTO.ConflictItem> conflicts, String riskLevel) {
        List<String> suggestions = new ArrayList<>();

        if (conflicts == null || conflicts.isEmpty()) {
            suggestions.add("这道菜的食材与您的饮食偏好没有冲突，可以放心食用！");
            return suggestions;
        }

        long allergyCount = conflicts.stream()
                .filter(c -> CONFLICT_TYPE_ALLERGY.equals(c.getConflictType()))
                .count();
        long dietaryCount = conflicts.stream()
                .filter(c -> CONFLICT_TYPE_DIETARY.equals(c.getConflictType()))
                .count();
        long dislikeCount = conflicts.stream()
                .filter(c -> CONFLICT_TYPE_DISLIKE.equals(c.getConflictType()))
                .count();

        if (allergyCount > 0) {
            suggestions.add("注意：检测到" + allergyCount + "种过敏食材，强烈建议更换菜谱或替换相关食材");
        }
        if (dietaryCount > 0) {
            suggestions.add("提示：有" + dietaryCount + "种食材不符合您的饮食限制，可考虑替换");
        }
        if (dislikeCount > 0) {
            suggestions.add("说明：有" + dislikeCount + "种食材是您不喜欢的，可根据个人口味调整");
        }

        switch (riskLevel) {
            case RISK_LEVEL_HIGH:
                suggestions.add("高风险：建议更换其他菜谱，或在专业指导下替换所有高风险食材");
                break;
            case RISK_LEVEL_MEDIUM:
                suggestions.add("中等风险：可通过替换相应食材来适应您的饮食需求");
                break;
            case RISK_LEVEL_LOW:
                suggestions.add("低风险：根据个人喜好选择是否替换，不影响健康");
                break;
        }

        suggestions.add("替换原则：尽量选择同类食材替代，保持菜品的口感和营养均衡");

        return suggestions;
    }
}
