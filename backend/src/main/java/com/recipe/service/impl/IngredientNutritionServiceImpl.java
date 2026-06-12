package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.dto.RecipeNutritionDTO;
import com.recipe.entity.IngredientNutrition;
import com.recipe.entity.Recipe;
import com.recipe.mapper.IngredientNutritionMapper;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.IngredientNutritionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientNutritionServiceImpl implements IngredientNutritionService {

    private final IngredientNutritionMapper nutritionMapper;
    private final RecipeMapper recipeMapper;
    private final ObjectMapper objectMapper;

    private static final int MIN_PAGE = 1;
    private static final int MAX_PAGE = 1000;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, BigDecimal> UNIT_TO_GRAM = new HashMap<>();

    static {
        UNIT_TO_GRAM.put("g", BigDecimal.ONE);
        UNIT_TO_GRAM.put("克", BigDecimal.ONE);
        UNIT_TO_GRAM.put("kg", new BigDecimal("1000"));
        UNIT_TO_GRAM.put("千克", new BigDecimal("1000"));
        UNIT_TO_GRAM.put("斤", new BigDecimal("500"));
        UNIT_TO_GRAM.put("两", new BigDecimal("50"));
        UNIT_TO_GRAM.put("ml", BigDecimal.ONE);
        UNIT_TO_GRAM.put("毫升", BigDecimal.ONE);
        UNIT_TO_GRAM.put("l", new BigDecimal("1000"));
        UNIT_TO_GRAM.put("升", new BigDecimal("1000"));
        UNIT_TO_GRAM.put("勺", new BigDecimal("10"));
        UNIT_TO_GRAM.put("汤匙", new BigDecimal("15"));
        UNIT_TO_GRAM.put("小勺", new BigDecimal("5"));
        UNIT_TO_GRAM.put("茶匙", new BigDecimal("5"));
        UNIT_TO_GRAM.put("杯", new BigDecimal("240"));
        UNIT_TO_GRAM.put("个", new BigDecimal("50"));
        UNIT_TO_GRAM.put("只", new BigDecimal("50"));
        UNIT_TO_GRAM.put("块", new BigDecimal("30"));
        UNIT_TO_GRAM.put("片", new BigDecimal("10"));
        UNIT_TO_GRAM.put("把", new BigDecimal("100"));
        UNIT_TO_GRAM.put("颗", new BigDecimal("5"));
        UNIT_TO_GRAM.put("粒", new BigDecimal("1"));
        UNIT_TO_GRAM.put("根", new BigDecimal("20"));
        UNIT_TO_GRAM.put("条", new BigDecimal("30"));
        UNIT_TO_GRAM.put("瓣", new BigDecimal("5"));
    }

    @Override
    public Page<IngredientNutrition> getNutritionPage(Integer page, Integer size, String keyword, String category) {
        int current = (page != null && page >= MIN_PAGE) ? Math.min(page, MAX_PAGE) : MIN_PAGE;
        int pageSize = (size != null && size >= MIN_PAGE_SIZE) ? Math.min(size, MAX_PAGE_SIZE) : 10;

        LambdaQueryWrapper<IngredientNutrition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IngredientNutrition::getDeleted, 0);

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(IngredientNutrition::getName, keyword)
                    .or().like(IngredientNutrition::getAlias, keyword));
        }

        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(IngredientNutrition::getCategory, category);
        }

        wrapper.orderByAsc(IngredientNutrition::getCategory, IngredientNutrition::getName);

        return nutritionMapper.selectPage(new Page<>(current, pageSize), wrapper);
    }

    @Override
    public IngredientNutrition getNutritionById(Long id) {
        return nutritionMapper.selectById(id);
    }

    @Override
    public IngredientNutrition getNutritionByName(String name) {
        return nutritionMapper.selectByName(name);
    }

    @Override
    public List<IngredientNutrition> getAllNutritions() {
        LambdaQueryWrapper<IngredientNutrition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IngredientNutrition::getDeleted, 0)
                .orderByAsc(IngredientNutrition::getCategory, IngredientNutrition::getName);
        return nutritionMapper.selectList(wrapper);
    }

    @Override
    public List<String> getAllCategories() {
        return nutritionMapper.selectAllCategories();
    }

    @Override
    @Transactional
    public IngredientNutrition createNutrition(IngredientNutrition nutrition) {
        nutrition.setId(null);
        nutritionMapper.insert(nutrition);
        return nutrition;
    }

    @Override
    @Transactional
    public IngredientNutrition updateNutrition(Long id, IngredientNutrition nutrition) {
        nutrition.setId(id);
        nutritionMapper.updateById(nutrition);
        return nutrition;
    }

    @Override
    @Transactional
    public boolean deleteNutrition(Long id) {
        return nutritionMapper.deleteById(id) > 0;
    }

    @Override
    public RecipeNutritionDTO calculateRecipeNutrition(String ingredientsJson) {
        List<Map<String, Object>> ingredients = parseIngredients(ingredientsJson);
        return calculateNutrition(ingredients);
    }

    @Override
    public RecipeNutritionDTO calculateRecipeNutritionById(Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return createEmptyNutritionDTO();
        }
        return calculateRecipeNutrition(recipe.getIngredients());
    }

    private List<Map<String, Object>> parseIngredients(String ingredientsJson) {
        try {
            return objectMapper.readValue(ingredientsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析食材JSON失败", e);
            return List.of();
        }
    }

    private RecipeNutritionDTO calculateNutrition(List<Map<String, Object>> ingredients) {
        RecipeNutritionDTO result = new RecipeNutritionDTO();
        List<RecipeNutritionDTO.IngredientNutritionDetail> details = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();

        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalCarbohydrate = BigDecimal.ZERO;
        BigDecimal totalFiber = BigDecimal.ZERO;
        BigDecimal totalSodium = BigDecimal.ZERO;

        Set<String> ingredientNames = new HashSet<>();
        for (Map<String, Object> ing : ingredients) {
            String name = (String) ing.get("name");
            if (name != null && !name.trim().isEmpty()) {
                ingredientNames.add(name.trim());
            }
        }

        Map<String, IngredientNutrition> nutritionMap = new HashMap<>();
        if (!ingredientNames.isEmpty()) {
            List<IngredientNutrition> nutritions = nutritionMapper.selectByNames(new ArrayList<>(ingredientNames));
            for (IngredientNutrition n : nutritions) {
                nutritionMap.put(n.getName(), n);
            }
        }

        for (Map<String, Object> ing : ingredients) {
            RecipeNutritionDTO.IngredientNutritionDetail detail = new RecipeNutritionDTO.IngredientNutritionDetail();
            String name = (String) ing.get("name");
            String amount = (String) ing.get("amount");

            detail.setName(name);
            detail.setAmount(amount);

            IngredientNutrition nutrition = findBestMatch(name, nutritionMap);
            if (nutrition != null) {
                detail.setMatched(true);
                detail.setMatchName(nutrition.getName());
                detail.setUnit(nutrition.getUnit());

                BigDecimal quantityInGrams = parseAmountToGrams(amount, name);
                detail.setQuantity(quantityInGrams);

                BigDecimal ratio = quantityInGrams.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                detail.setCalories(nutrition.getCalories() != null ?
                        nutrition.getCalories().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                detail.setProtein(nutrition.getProtein() != null ?
                        nutrition.getProtein().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                detail.setFat(nutrition.getFat() != null ?
                        nutrition.getFat().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                detail.setCarbohydrate(nutrition.getCarbohydrate() != null ?
                        nutrition.getCarbohydrate().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                detail.setFiber(nutrition.getFiber() != null ?
                        nutrition.getFiber().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                detail.setSodium(nutrition.getSodium() != null ?
                        nutrition.getSodium().multiply(ratio).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

                totalCalories = totalCalories.add(detail.getCalories());
                totalProtein = totalProtein.add(detail.getProtein());
                totalFat = totalFat.add(detail.getFat());
                totalCarbohydrate = totalCarbohydrate.add(detail.getCarbohydrate());
                totalFiber = totalFiber.add(detail.getFiber());
                totalSodium = totalSodium.add(detail.getSodium());
            } else {
                detail.setMatched(false);
                detail.setCalories(BigDecimal.ZERO);
                detail.setProtein(BigDecimal.ZERO);
                detail.setFat(BigDecimal.ZERO);
                detail.setCarbohydrate(BigDecimal.ZERO);
                detail.setFiber(BigDecimal.ZERO);
                detail.setSodium(BigDecimal.ZERO);
                if (name != null && !name.trim().isEmpty()) {
                    unmatched.add(name);
                }
            }

            details.add(detail);
        }

        result.setTotalCalories(totalCalories.setScale(2, RoundingMode.HALF_UP));
        result.setTotalProtein(totalProtein.setScale(2, RoundingMode.HALF_UP));
        result.setTotalFat(totalFat.setScale(2, RoundingMode.HALF_UP));
        result.setTotalCarbohydrate(totalCarbohydrate.setScale(2, RoundingMode.HALF_UP));
        result.setTotalFiber(totalFiber.setScale(2, RoundingMode.HALF_UP));
        result.setTotalSodium(totalSodium.setScale(2, RoundingMode.HALF_UP));
        result.setIngredientDetails(details);
        result.setUnmatchedIngredients(unmatched);

        return result;
    }

    private IngredientNutrition findBestMatch(String name, Map<String, IngredientNutrition> nutritionMap) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmedName = name.trim();

        IngredientNutrition exact = nutritionMap.get(trimmedName);
        if (exact != null) {
            return exact;
        }

        for (Map.Entry<String, IngredientNutrition> entry : nutritionMap.entrySet()) {
            String key = entry.getKey();
            if (key.contains(trimmedName) || trimmedName.contains(key)) {
                return entry.getValue();
            }
            IngredientNutrition n = entry.getValue();
            if (n.getAlias() != null && !n.getAlias().isEmpty()) {
                String[] aliases = n.getAlias().split(",");
                for (String alias : aliases) {
                    if (alias.trim().equals(trimmedName) || trimmedName.contains(alias.trim()) || alias.trim().contains(trimmedName)) {
                        return n;
                    }
                }
            }
        }

        return null;
    }

    private BigDecimal parseAmountToGrams(String amount, String ingredientName) {
        if (amount == null || amount.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        amount = amount.trim().toLowerCase();

        Pattern pattern = Pattern.compile("([\\d.]+)\\s*([\\u4e00-\\u9fa5a-zA-Z]+)?");
        Matcher matcher = pattern.matcher(amount);

        if (matcher.find()) {
            try {
                BigDecimal quantity = new BigDecimal(matcher.group(1));
                String unit = matcher.group(2);

                if (unit == null || unit.isEmpty()) {
                    unit = "g";
                }

                unit = unit.toLowerCase();

                BigDecimal multiplier = UNIT_TO_GRAM.get(unit);
                if (multiplier != null) {
                    return quantity.multiply(multiplier);
                }

                return quantity;
            } catch (NumberFormatException e) {
                log.warn("解析食材用量失败: amount={}, name={}", amount, ingredientName);
            }
        }

        Pattern numPattern = Pattern.compile("([\\d.]+)");
        Matcher numMatcher = numPattern.matcher(amount);
        if (numMatcher.find()) {
            try {
                return new BigDecimal(numMatcher.group(1));
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }

        if (amount.contains("适量") || amount.contains("少许") || amount.contains("少量")) {
            return new BigDecimal("5");
        }

        return BigDecimal.ZERO;
    }

    private RecipeNutritionDTO createEmptyNutritionDTO() {
        RecipeNutritionDTO dto = new RecipeNutritionDTO();
        dto.setTotalCalories(BigDecimal.ZERO);
        dto.setTotalProtein(BigDecimal.ZERO);
        dto.setTotalFat(BigDecimal.ZERO);
        dto.setTotalCarbohydrate(BigDecimal.ZERO);
        dto.setTotalFiber(BigDecimal.ZERO);
        dto.setTotalSodium(BigDecimal.ZERO);
        dto.setIngredientDetails(List.of());
        dto.setUnmatchedIngredients(List.of());
        return dto;
    }
}
