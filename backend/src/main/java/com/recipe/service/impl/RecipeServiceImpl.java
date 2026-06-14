package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.dto.FlavorAdjustmentDTO;
import com.recipe.dto.RecipeDTO;
import com.recipe.dto.RecipeDetailDTO;
import com.recipe.entity.Recipe;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.FamilyTasteProfileService;
import com.recipe.service.RecipeService;
import com.recipe.service.ViewHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeMapper recipeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ViewHistoryService viewHistoryService;
    private final FamilyTasteProfileService familyTasteProfileService;

    private static final String HOT_RECIPE_KEY_PREFIX = "recipe:rank:v2:";
    private static final String HOT_RECIPE_KEY_WEEKLY = HOT_RECIPE_KEY_PREFIX + "weekly";
    private static final String HOT_RECIPE_KEY_MONTHLY = HOT_RECIPE_KEY_PREFIX + "monthly";
    private static final String HOT_RECIPE_KEY_TOTAL = HOT_RECIPE_KEY_PREFIX + "total";

    private static final String DIMENSION_WEEKLY = "weekly";
    private static final String DIMENSION_MONTHLY = "monthly";
    private static final String DIMENSION_TOTAL = "total";
    private static final List<String> VALID_DIMENSIONS = List.of(DIMENSION_WEEKLY, DIMENSION_MONTHLY, DIMENSION_TOTAL);

    private static final int MIN_PAGE = 1;
    private static final int MAX_PAGE = 1000;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String OLD_HOT_RECIPE_KEY_LEGACY = "recipe:hot:monthly";

    @Value("${recipe.cache.hot-recipe-expire:86400}")
    private Long hotRecipeExpire;

    @jakarta.annotation.PostConstruct
    public void cleanLegacyCacheKeys() {
        try {
            redisTemplate.delete(OLD_HOT_RECIPE_KEY_LEGACY);
            redisTemplate.delete("recipe:hot:weekly");
            redisTemplate.delete("recipe:hot:monthly");
            redisTemplate.delete("recipe:hot:total");
            log.info("已清理旧版热门菜谱缓存key");
        } catch (Exception e) {
            log.warn("清理旧版缓存key失败", e);
        }
    }

    @Override
    public List<Recipe> getHotRecipes() {
        return getHotRecipes(DIMENSION_TOTAL);
    }

    @Override
    public List<Recipe> getHotRecipes(String dimension) {
        if (!VALID_DIMENSIONS.contains(dimension)) {
            dimension = DIMENSION_TOTAL;
        }
        String cacheKey = resolveCacheKey(dimension);
        String stampKey = cacheKey + ":stamp";

        Boolean hasKey = redisTemplate.hasKey(cacheKey);
        Boolean hasStamp = redisTemplate.hasKey(stampKey);
        if (Boolean.TRUE.equals(hasKey) || Boolean.TRUE.equals(hasStamp)) {
            Set<ZSetOperations.TypedTuple<Object>> cached = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(cacheKey, 0, 9);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream()
                        .map(tuple -> {
                            try {
                                return objectMapper.convertValue(tuple.getValue(), Recipe.class);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(r -> r != null)
                        .toList();
            }
            return List.of();
        }

        List<Recipe> recipes = fetchHotRecipesByDimension(dimension, 10);
        cacheHotRecipes(cacheKey, recipes);
        return recipes;
    }

    private String resolveCacheKey(String dimension) {
        if (DIMENSION_WEEKLY.equals(dimension)) {
            return HOT_RECIPE_KEY_WEEKLY;
        } else if (DIMENSION_MONTHLY.equals(dimension)) {
            return HOT_RECIPE_KEY_MONTHLY;
        } else {
            return HOT_RECIPE_KEY_TOTAL;
        }
    }

    private List<Recipe> fetchHotRecipesByDimension(String dimension, Integer limit) {
        if (DIMENSION_WEEKLY.equals(dimension)) {
            return recipeMapper.selectWeeklyHotRecipes(limit);
        } else if (DIMENSION_MONTHLY.equals(dimension)) {
            return recipeMapper.selectMonthlyHotRecipes(limit);
        } else {
            return recipeMapper.selectHotRecipes(limit);
        }
    }

    private void cacheHotRecipes(String cacheKey, List<Recipe> recipes) {
        redisTemplate.delete(List.of(cacheKey, cacheKey + ":stamp"));
        if (recipes != null && !recipes.isEmpty()) {
            recipes.forEach(recipe -> {
                redisTemplate.opsForZSet().add(cacheKey, recipe,
                        recipe.getFavoriteCount() != null ? recipe.getFavoriteCount().doubleValue() : 0D);
            });
        }
        redisTemplate.opsForValue().set(cacheKey + ":stamp", System.currentTimeMillis(), hotRecipeExpire, TimeUnit.SECONDS);
        redisTemplate.expire(cacheKey, hotRecipeExpire, TimeUnit.SECONDS);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshHotRecipes() {
        log.info("开始刷新热门菜谱缓存...");

        List<Recipe> weeklyRecipes = recipeMapper.selectWeeklyHotRecipes(10);
        cacheHotRecipes(HOT_RECIPE_KEY_WEEKLY, weeklyRecipes);
        log.info("周榜缓存刷新完成，共{}条", weeklyRecipes.size());

        List<Recipe> monthlyRecipes = recipeMapper.selectMonthlyHotRecipes(10);
        cacheHotRecipes(HOT_RECIPE_KEY_MONTHLY, monthlyRecipes);
        log.info("月榜缓存刷新完成，共{}条", monthlyRecipes.size());

        List<Recipe> totalRecipes = recipeMapper.selectHotRecipes(10);
        cacheHotRecipes(HOT_RECIPE_KEY_TOTAL, totalRecipes);
        log.info("总榜缓存刷新完成，共{}条", totalRecipes.size());

        log.info("热门菜谱缓存全部刷新完成");
    }

    @Override
    public void updateHotRecipeCache(Long recipeId, Integer delta) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            return;
        }

        String[] cacheKeys = new String[]{
                HOT_RECIPE_KEY_TOTAL,
                HOT_RECIPE_KEY_WEEKLY,
                HOT_RECIPE_KEY_MONTHLY
        };

        for (String cacheKey : cacheKeys) {
            try {
                Boolean hasKey = redisTemplate.hasKey(cacheKey);
                if (Boolean.TRUE.equals(hasKey)) {
                    Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                            .rangeWithScores(cacheKey, 0, -1);
                    if (tuples != null) {
                        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                            Object value = tuple.getValue();
                            if (value != null) {
                                try {
                                    Recipe cachedRecipe = objectMapper.convertValue(value, Recipe.class);
                                    if (cachedRecipe != null && recipeId.equals(cachedRecipe.getId())) {
                                        Double currentScore = tuple.getScore() != null ? tuple.getScore() : 0D;
                                        Double newScore = Math.max(0, currentScore + delta);
                                        redisTemplate.opsForZSet().remove(cacheKey, value);
                                        cachedRecipe.setFavoriteCount(recipe.getFavoriteCount());
                                        redisTemplate.opsForZSet().add(cacheKey, cachedRecipe, newScore);
                                        break;
                                    }
                                } catch (Exception e) {
                                    log.warn("转换缓存菜谱对象失败", e);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("更新热门菜谱缓存失败, cacheKey={}, recipeId={}", cacheKey, recipeId, e);
            }
        }
    }

    @Override
    public Page<Recipe> getRecipePage(Integer page, Integer size, String keyword, String cuisine, String scene, Integer difficulty) {
        int current = (page != null && page >= MIN_PAGE) ? Math.min(page, MAX_PAGE) : MIN_PAGE;
        int pageSize = (size != null && size >= MIN_PAGE_SIZE) ? Math.min(size, MAX_PAGE_SIZE) : 10;

        if (keyword != null && !keyword.trim().isEmpty()) {
            return searchRecipesWithWeight(current, pageSize, keyword, cuisine, scene, difficulty);
        }

        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recipe::getStatus, 1)
                .eq(Recipe::getIsDraft, 0)
                .eq(Recipe::getDeleted, 0);

        if (cuisine != null && !cuisine.isEmpty()) {
            wrapper.like(Recipe::getTags, cuisine);
        }

        if (scene != null && !scene.isEmpty()) {
            wrapper.like(Recipe::getTags, scene);
        }

        if (difficulty != null) {
            wrapper.eq(Recipe::getDifficulty, difficulty);
        }

        wrapper.orderByDesc(Recipe::getFavoriteCount);

        return recipeMapper.selectPage(new Page<>(current, pageSize), wrapper);
    }

    private Page<Recipe> searchRecipesWithWeight(Integer page, Integer size, String keyword,
                                                  String cuisine, String scene, Integer difficulty) {
        int current = (page != null && page >= MIN_PAGE) ? Math.min(page, MAX_PAGE) : MIN_PAGE;
        int pageSize = (size != null && size >= MIN_PAGE_SIZE) ? Math.min(size, MAX_PAGE_SIZE) : 10;

        List<Recipe> allRecipes = recipeMapper.searchRecipesWithWeight(keyword, cuisine, scene, difficulty);

        int total = allRecipes.size();
        int fromIndex = (current - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        Page<Recipe> result = new Page<>(current, pageSize, total);
        if (fromIndex >= total || fromIndex < 0) {
            result.setRecords(List.of());
            return result;
        }
        result.setRecords(allRecipes.subList(fromIndex, toIndex));
        return result;
    }

    @Override
    public Recipe getRecipeDetail(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe != null) {
            recipe.setViewCount(recipe.getViewCount() == null ? 1 : recipe.getViewCount() + 1);
            recipeMapper.updateById(recipe);
            try {
                viewHistoryService.addViewHistory(id);
            } catch (Exception e) {
                log.warn("记录浏览历史失败，recipeId={}", id, e);
            }
        }
        return recipe;
    }

    @Override
    public RecipeDetailDTO getRecipeDetailWithAdjustment(Long id) {
        return getRecipeDetailWithAdjustment(id, null);
    }

    @Override
    public RecipeDetailDTO getRecipeDetailWithAdjustment(Long id, Long profileId) {
        Recipe recipe = getRecipeDetail(id);
        if (recipe == null) {
            return null;
        }

        RecipeDetailDTO dto = new RecipeDetailDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setDescription(recipe.getDescription());
        dto.setCoverImage(recipe.getCoverImage());
        dto.setAuthor(recipe.getAuthor());
        dto.setCookTime(recipe.getCookTime());
        dto.setDifficulty(recipe.getDifficulty());
        dto.setFavoriteCount(recipe.getFavoriteCount());
        dto.setViewCount(recipe.getViewCount());
        dto.setAverageRating(recipe.getAverageRating());
        dto.setRatingCount(recipe.getRatingCount());
        dto.setTags(recipe.getTags());
        dto.setIngredients(recipe.getIngredients());
        dto.setSteps(recipe.getSteps());
        dto.setTips(recipe.getTips());
        dto.setStatus(recipe.getStatus());
        dto.setIsDraft(recipe.getIsDraft());
        dto.setCreatedAt(recipe.getCreatedAt());
        dto.setUpdatedAt(recipe.getUpdatedAt());
        dto.setDeleted(recipe.getDeleted());
        dto.setViewTime(recipe.getViewTime());

        try {
            FlavorAdjustmentDTO adjustment;
            if (profileId != null) {
                adjustment = familyTasteProfileService.getFlavorAdjustmentByProfile(id, profileId);
            } else {
                adjustment = familyTasteProfileService.getFlavorAdjustment(id);
            }
            dto.setFlavorAdjustment(adjustment);
        } catch (Exception e) {
            log.warn("获取调味调整建议失败，recipeId={}", id, e);
        }

        return dto;
    }

    @Override
    @Transactional
    public Recipe createRecipe(RecipeDTO dto) {
        Recipe recipe = convertToEntity(dto);
        recipe.setIsDraft(0);
        recipe.setStatus(1);
        recipe.setFavoriteCount(0);
        recipe.setViewCount(0);
        recipeMapper.insert(recipe);
        return recipe;
    }

    @Override
    @Transactional
    public Recipe updateRecipe(Long id, RecipeDTO dto) {
        Recipe recipe = convertToEntity(dto);
        recipe.setId(id);
        recipeMapper.updateById(recipe);
        return recipe;
    }

    @Override
    @Transactional
    public boolean deleteRecipe(Long id) {
        return recipeMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public Recipe saveDraft(RecipeDTO dto) {
        Recipe recipe = convertToEntity(dto);
        recipe.setIsDraft(1);
        recipe.setStatus(0);
        if (recipe.getFavoriteCount() == null) recipe.setFavoriteCount(0);
        if (recipe.getViewCount() == null) recipe.setViewCount(0);
        recipeMapper.insert(recipe);
        return recipe;
    }

    @Override
    public List<Recipe> getDrafts() {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recipe::getIsDraft, 1)
                .eq(Recipe::getDeleted, 0)
                .orderByDesc(Recipe::getUpdatedAt);
        return recipeMapper.selectList(wrapper);
    }

    @Override
    public List<Recipe> getUserRecipes() {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recipe::getIsDraft, 0)
                .eq(Recipe::getStatus, 1)
                .eq(Recipe::getDeleted, 0)
                .orderByDesc(Recipe::getCreatedAt);
        return recipeMapper.selectList(wrapper);
    }

    private Recipe convertToEntity(RecipeDTO dto) {
        Recipe recipe = new Recipe();
        recipe.setTitle(dto.getTitle());
        recipe.setDescription(dto.getDescription());
        recipe.setCoverImage(dto.getCoverImage());
        recipe.setAuthor(dto.getAuthor());
        recipe.setCookTime(dto.getCookTime());
        recipe.setDifficulty(dto.getDifficulty());
        
        if (dto.getTags() != null) {
            recipe.setTags(String.join(",", dto.getTags()));
        }
        
        try {
            if (dto.getIngredients() != null) {
                recipe.setIngredients(objectMapper.writeValueAsString(dto.getIngredients()));
            }
            if (dto.getSteps() != null) {
                recipe.setSteps(objectMapper.writeValueAsString(dto.getSteps()));
            }
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
        }
        
        recipe.setTips(dto.getTips());
        return recipe;
    }

    @Override
    public List<Recipe> getSimilarRecipes(Long recipeId, Integer limit) {
        Recipe currentRecipe = recipeMapper.selectById(recipeId);
        if (currentRecipe == null || currentRecipe.getTags() == null || currentRecipe.getTags().isEmpty()) {
            return recipeMapper.selectHotRecipes(limit != null ? limit : 6);
        }

        Set<String> currentTags = Arrays.stream(currentRecipe.getTags().split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());

        if (currentTags.isEmpty()) {
            return recipeMapper.selectHotRecipes(limit != null ? limit : 6);
        }

        List<Recipe> candidates = recipeMapper.selectSimilarCandidates(recipeId);

        List<RecipeWithScore> scored = candidates.stream()
                .map(r -> {
                    Set<String> recipeTags = Arrays.stream(r.getTags().split(","))
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .collect(Collectors.toSet());

                    double tagScore = 0;
                    for (String tag : currentTags) {
                        if (recipeTags.contains(tag)) {
                            tagScore += 1.0;
                        } else {
                            for (String rt : recipeTags) {
                                if (rt.contains(tag) || tag.contains(rt)) {
                                    tagScore += 0.5;
                                    break;
                                }
                            }
                        }
                    }

                    double tagSimilarity = currentTags.isEmpty() ? 0 : tagScore / currentTags.size();

                    double difficultyBonus = 0;
                    if (currentRecipe.getDifficulty() != null && r.getDifficulty() != null) {
                        int diff = Math.abs(currentRecipe.getDifficulty() - r.getDifficulty());
                        if (diff == 0) difficultyBonus = 0.1;
                        else if (diff == 1) difficultyBonus = 0.05;
                    }

                    double cookTimeBonus = 0;
                    if (currentRecipe.getCookTime() != null && r.getCookTime() != null) {
                        double ratio = Math.min(currentRecipe.getCookTime(), r.getCookTime())
                                / (double) Math.max(currentRecipe.getCookTime(), r.getCookTime());
                        cookTimeBonus = ratio * 0.1;
                    }

                    double popularityBonus = (r.getFavoriteCount() != null ? r.getFavoriteCount() : 0) / 10000.0;
                    popularityBonus = Math.min(popularityBonus, 0.1);

                    double totalScore = tagSimilarity * 0.7 + difficultyBonus + cookTimeBonus + popularityBonus;

                    return new RecipeWithScore(r, totalScore);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit != null ? limit : 6)
                .collect(Collectors.toList());

        if (scored.isEmpty()) {
            return recipeMapper.selectHotRecipes(limit != null ? limit : 6);
        }

        List<Recipe> result = scored.stream()
                .map(rs -> rs.recipe)
                .collect(Collectors.toList());

        int needMore = (limit != null ? limit : 6) - result.size();
        if (needMore > 0) {
            Set<Long> existingIds = result.stream().map(Recipe::getId).collect(Collectors.toSet());
            List<Recipe> hotRecipes = recipeMapper.selectHotRecipes(needMore + existingIds.size());
            for (Recipe hot : hotRecipes) {
                if (!existingIds.contains(hot.getId()) && !hot.getId().equals(recipeId)) {
                    result.add(hot);
                    existingIds.add(hot.getId());
                    if (result.size() >= (limit != null ? limit : 6)) break;
                }
            }
        }

        return result;
    }

    private static class RecipeWithScore {
        final Recipe recipe;
        final double score;

        RecipeWithScore(Recipe recipe, double score) {
            this.recipe = recipe;
            this.score = score;
        }
    }
}
