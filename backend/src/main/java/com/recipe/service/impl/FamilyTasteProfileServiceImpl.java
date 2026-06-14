package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.dto.FlavorAdjustmentDTO;
import com.recipe.entity.FamilyTasteProfile;
import com.recipe.entity.Recipe;
import com.recipe.mapper.FamilyTasteProfileMapper;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.FamilyTasteProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyTasteProfileServiceImpl implements FamilyTasteProfileService {

    private final FamilyTasteProfileMapper profileMapper;
    private final RecipeMapper recipeMapper;
    private final ObjectMapper objectMapper;

    private static final Long DEFAULT_USER_ID = 1L;

    private static final List<String> OIL_KEYWORDS = Arrays.asList("油", "食用油", "色拉油", "植物油", "花生油", "橄榄油", "玉米油", "菜籽油", "香油", "麻油");
    private static final List<String> SALT_KEYWORDS = Arrays.asList("盐", "食盐", "精盐", "海盐");
    private static final List<String> SUGAR_KEYWORDS = Arrays.asList("糖", "白糖", "冰糖", "砂糖", "红糖", "蜂蜜", "蔗糖");
    private static final List<String> SOY_SAUCE_KEYWORDS = Arrays.asList("生抽", "老抽", "酱油", "味极鲜", "豉油", "蒸鱼豉油");
    private static final List<String> SPICY_KEYWORDS = Arrays.asList("辣椒", "花椒", "干辣椒", "辣椒粉", "辣椒油", "小米辣", "剁椒", "藤椒", "青花椒", "火锅底料", "麻辣");
    private static final List<String> HIGH_SODIUM_KEYWORDS = Arrays.asList("豆瓣酱", "豆豉", "咸菜", "腌菜", "泡菜", "榨菜", "梅菜", "腊", "火腿", "香肠", "培根", "酱");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([\\d.]+)\\s*(勺|汤匙|茶匙|ml|毫升|g|克|少许|适量|少量|大半勺|小勺|大勺)?");

    @Override
    public List<FamilyTasteProfile> getProfiles() {
        LambdaQueryWrapper<FamilyTasteProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyTasteProfile::getUserId, DEFAULT_USER_ID)
                .eq(FamilyTasteProfile::getDeleted, 0)
                .orderByDesc(FamilyTasteProfile::getIsDefault)
                .orderByDesc(FamilyTasteProfile::getUpdatedAt);
        return profileMapper.selectList(wrapper);
    }

    @Override
    public FamilyTasteProfile getDefaultProfile() {
        LambdaQueryWrapper<FamilyTasteProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyTasteProfile::getUserId, DEFAULT_USER_ID)
                .eq(FamilyTasteProfile::getIsDefault, 1)
                .eq(FamilyTasteProfile::getDeleted, 0)
                .last("LIMIT 1");
        FamilyTasteProfile profile = profileMapper.selectOne(wrapper);
        if (profile == null) {
            profile = createDefaultProfile();
        }
        return profile;
    }

    @Override
    public FamilyTasteProfile getProfileById(Long id) {
        return profileMapper.selectById(id);
    }

    @Override
    @Transactional
    public FamilyTasteProfile createProfile(FamilyTasteProfile profile) {
        profile.setUserId(DEFAULT_USER_ID);
        if (profile.getIsDefault() != null && profile.getIsDefault() == 1) {
            clearDefaultFlag();
        }
        if (profile.getIsDefault() == null) {
            profile.setIsDefault(0);
        }
        profileMapper.insert(profile);
        return profile;
    }

    @Override
    @Transactional
    public FamilyTasteProfile updateProfile(Long id, FamilyTasteProfile profile) {
        profile.setId(id);
        profile.setUserId(DEFAULT_USER_ID);
        if (profile.getIsDefault() != null && profile.getIsDefault() == 1) {
            clearDefaultFlag();
        }
        profileMapper.updateById(profile);
        return profileMapper.selectById(id);
    }

    @Override
    @Transactional
    public boolean deleteProfile(Long id) {
        return profileMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean setDefaultProfile(Long id) {
        clearDefaultFlag();
        FamilyTasteProfile profile = profileMapper.selectById(id);
        if (profile != null) {
            profile.setIsDefault(1);
            profileMapper.updateById(profile);
            return true;
        }
        return false;
    }

    private void clearDefaultFlag() {
        LambdaQueryWrapper<FamilyTasteProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyTasteProfile::getUserId, DEFAULT_USER_ID)
                .eq(FamilyTasteProfile::getIsDefault, 1);
        List<FamilyTasteProfile> defaults = profileMapper.selectList(wrapper);
        for (FamilyTasteProfile p : defaults) {
            p.setIsDefault(0);
            profileMapper.updateById(p);
        }
    }

    private FamilyTasteProfile createDefaultProfile() {
        FamilyTasteProfile profile = new FamilyTasteProfile();
        profile.setUserId(DEFAULT_USER_ID);
        profile.setProfileName("默认口味");
        profile.setLowOil(0);
        profile.setLowSalt(0);
        profile.setLowSugar(0);
        profile.setNoSpicy(0);
        profile.setLightSpicy(0);
        profile.setMediumSpicy(0);
        profile.setHeavySpicy(1);
        profile.setIsDefault(1);
        profileMapper.insert(profile);
        return profile;
    }

    @Override
    public FlavorAdjustmentDTO getFlavorAdjustment(Long recipeId) {
        FamilyTasteProfile profile = getDefaultProfile();
        return generateFlavorAdjustment(recipeId, profile);
    }

    @Override
    public FlavorAdjustmentDTO getFlavorAdjustmentByProfile(Long recipeId, Long profileId) {
        FamilyTasteProfile profile = profileMapper.selectById(profileId);
        if (profile == null) {
            profile = getDefaultProfile();
        }
        return generateFlavorAdjustment(recipeId, profile);
    }

    private FlavorAdjustmentDTO generateFlavorAdjustment(Long recipeId, FamilyTasteProfile profile) {
        FlavorAdjustmentDTO result = new FlavorAdjustmentDTO();
        List<FlavorAdjustmentDTO.SeasoningTip> seasoningTips = new ArrayList<>();
        List<FlavorAdjustmentDTO.CookingSuggestion> cookingSuggestions = new ArrayList<>();
        List<String> summary = new ArrayList<>();

        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            result.setHasAdjustment(false);
            result.setSeasoningTips(Collections.emptyList());
            result.setCookingSuggestions(Collections.emptyList());
            result.setSummary(Collections.singletonList("菜谱不存在"));
            return result;
        }

        List<Map<String, Object>> ingredients = parseIngredients(recipe.getIngredients());
        List<Map<String, Object>> steps = parseSteps(recipe.getSteps());

        if (profile.getLowOil() != null && profile.getLowOil() == 1) {
            handleLowOil(ingredients, steps, seasoningTips, cookingSuggestions, summary);
        }

        if (profile.getLowSalt() != null && profile.getLowSalt() == 1) {
            handleLowSalt(ingredients, steps, seasoningTips, cookingSuggestions, summary);
        }

        if (profile.getLowSugar() != null && profile.getLowSugar() == 1) {
            handleLowSugar(ingredients, steps, seasoningTips, cookingSuggestions, summary);
        }

        if (profile.getNoSpicy() != null && profile.getNoSpicy() == 1) {
            handleNoSpicy(ingredients, steps, seasoningTips, cookingSuggestions, summary);
        } else if (profile.getLightSpicy() != null && profile.getLightSpicy() == 1) {
            handleLightSpicy(ingredients, steps, seasoningTips, cookingSuggestions, summary);
        }

        String tastePrefs = profile.getTastePreferences();
        if (tastePrefs != null && !tastePrefs.isEmpty()) {
            summary.add("您的口味偏好：" + tastePrefs);
        }

        String notes = profile.getDietaryNotes();
        if (notes != null && !notes.isEmpty()) {
            summary.add("备注：" + notes);
        }

        boolean hasAdjustment = !seasoningTips.isEmpty() || !cookingSuggestions.isEmpty();
        result.setHasAdjustment(hasAdjustment);
        result.setSeasoningTips(seasoningTips);
        result.setCookingSuggestions(cookingSuggestions);

        if (summary.isEmpty()) {
            summary.add("当前菜谱无需根据您的口味进行调整，按原方烹饪即可");
        } else if (hasAdjustment) {
            summary.add(0, "根据您的家庭口味档案，为您提供以下调味调整建议：");
        }

        result.setSummary(summary);
        return result;
    }

    private List<Map<String, Object>> parseIngredients(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析食材JSON失败", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> parseSteps(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析步骤JSON失败", e);
            return Collections.emptyList();
        }
    }

    private void handleLowOil(List<Map<String, Object>> ingredients, List<Map<String, Object>> steps,
                              List<FlavorAdjustmentDTO.SeasoningTip> tips,
                              List<FlavorAdjustmentDTO.CookingSuggestion> suggestions,
                              List<String> summary) {
        boolean found = false;
        for (Map<String, Object> ing : ingredients) {
            String name = getString(ing, "name");
            String amount = getString(ing, "amount");
            if (name != null && containsAny(name, OIL_KEYWORDS)) {
                FlavorAdjustmentDTO.SeasoningTip tip = new FlavorAdjustmentDTO.SeasoningTip();
                tip.setSeasoningName(name);
                tip.setOriginalAmount(amount != null ? amount : "适量");
                tip.setSuggestedAmount(reduceAmount(amount, 0.5));
                tip.setAdjustType("少油");
                tip.setReason("您偏好少油，建议将用油量减少一半");
                tips.add(tip);
                found = true;
            }
        }
        if (found) {
            summary.add("少油建议：所有油类用量建议减半");
            for (int i = 0; i < steps.size(); i++) {
                String content = getString(steps.get(i), "content");
                if (content != null && (content.contains("油") && (content.contains("炒") || content.contains("煎") || content.contains("炸")))) {
                    FlavorAdjustmentDTO.CookingSuggestion sugg = new FlavorAdjustmentDTO.CookingSuggestion();
                    sugg.setStepIndex(String.valueOf(i + 1));
                    sugg.setOriginalContent(content);
                    sugg.setSuggestedContent(content.replace("大火", "中小火").replace("油炸", "煎").replace("煎至", "小火慢煎至"));
                    sugg.setAdjustType("少油烹饪");
                    sugg.setReason("少油时建议用中小火，避免高温产生有害物质，可改用不粘锅减少用油量");
                    suggestions.add(sugg);
                }
            }
        }
    }

    private void handleLowSalt(List<Map<String, Object>> ingredients, List<Map<String, Object>> steps,
                               List<FlavorAdjustmentDTO.SeasoningTip> tips,
                               List<FlavorAdjustmentDTO.CookingSuggestion> suggestions,
                               List<String> summary) {
        boolean found = false;
        for (Map<String, Object> ing : ingredients) {
            String name = getString(ing, "name");
            String amount = getString(ing, "amount");
            if (name != null) {
                if (containsAny(name, SALT_KEYWORDS)) {
                    FlavorAdjustmentDTO.SeasoningTip tip = new FlavorAdjustmentDTO.SeasoningTip();
                    tip.setSeasoningName(name);
                    tip.setOriginalAmount(amount != null ? amount : "适量");
                    tip.setSuggestedAmount(reduceAmount(amount, 0.5));
                    tip.setAdjustType("低盐");
                    tip.setReason("您偏好低盐，建议将盐用量减少一半，可用柠檬汁、醋等提味");
                    tips.add(tip);
                    found = true;
                }
                if (containsAny(name, SOY_SAUCE_KEYWORDS)) {
                    FlavorAdjustmentDTO.SeasoningTip tip = new FlavorAdjustmentDTO.SeasoningTip();
                    tip.setSeasoningName(name);
                    tip.setOriginalAmount(amount != null ? amount : "适量");
                    tip.setSuggestedAmount(reduceAmount(amount, 0.7));
                    tip.setAdjustType("低盐");
                    tip.setReason("酱油含钠量高，建议减少用量约30%，或选用低盐酱油");
                    tips.add(tip);
                    found = true;
                }
                if (containsAny(name, HIGH_SODIUM_KEYWORDS)) {
                    FlavorAdjustmentDTO.SeasoningTip tip = new FlavorAdjustmentDTO.SeasoningTip();
                    tip.setSeasoningName(name);
                    tip.setOriginalAmount(amount != null ? amount : "适量");
                    tip.setSuggestedAmount(reduceAmount(amount, 0.6));
                    tip.setAdjustType("低盐");
                    tip.setReason(name + "本身含有较高盐分，建议减少用量或提前浸泡去盐");
                    tips.add(tip);
                    found = true;
                }
            }
        }
        if (found) {
            summary.add("低盐建议：盐及含盐调味品用量建议减少，可多利用葱姜蒜、香料、醋、柠檬汁等天然调味增香");
            for (int i = 0; i < steps.size(); i++) {
                String content = getString(steps.get(i), "content");
                if (content != null && (content.contains("盐") || content.contains("调味"))) {
                    FlavorAdjustmentDTO.CookingSuggestion sugg = new FlavorAdjustmentDTO.CookingSuggestion();
                    sugg.setStepIndex(String.valueOf(i + 1));
                    sugg.setOriginalContent(content);
                    sugg.setSuggestedContent(content + "（注意少盐，可用葱姜蒜、香料增香提味）");
                    sugg.setAdjustType("低盐调味");
                    sugg.setReason("减少盐摄入更健康，可借助天然香料提升风味");
                    suggestions.add(sugg);
                }
            }
        }
    }

    private void handleLowSugar(List<Map<String, Object>> ingredients, List<Map<String, Object>> steps,
                                List<FlavorAdjustmentDTO.SeasoningTip> tips,
                                List<FlavorAdjustmentDTO.CookingSuggestion> suggestions,
                                List<String> summary) {
        boolean found = false;
        for (Map<String, Object> ing : ingredients) {
            String name = getString(ing, "name");
            String amount = getString(ing, "amount");
            if (name != null && containsAny(name, SUGAR_KEYWORDS)) {
                FlavorAdjustmentDTO.SeasoningTip tip = new FlavorAdjustmentDTO.SeasoningTip();
                tip.setSeasoningName(name);
                tip.setOriginalAmount(amount != null ? amount : "适量");
                tip.setSuggestedAmount(reduceAmount(amount, 0.3));
                tip.setAdjustType("低糖");
                tip.setReason("您偏好低糖，建议将糖用量减少约70%，可用木糖醇或天然甜味替代");
                tips.add(tip);
                found = true;
            }
        }
        if (found) {
            summary.add("低糖建议：糖用量建议大幅减少，可使用木糖醇、甜菊糖等代糖，或利用食材本身的甜味");
        }
    }

    private void handleNoSpicy(List<Map<String, Object>> ingredients, List<Map<String, Object>> steps,
                               List<FlavorAdjustmentDTO.SeasoningTip> tips,
                               List<FlavorAdjustmentDTO.CookingSuggestion> suggestions,
                               List<String> summary) {
        boolean found = false;
        for (Map<String, Object> ing : ingredients) {
            String name = getString(ing, "name");
            String amount = getString(ing, "amount");
            if (name != null && containsAny(name, SPICY_KEYWORDS)) {
                FlavorAdjustmentDTO.SeasoningTip tip = new FlavorAdjustmentDTO.SeasoningTip();
                tip.setSeasoningName(name);
                tip.setOriginalAmount(amount != null ? amount : "适量");
                tip.setSuggestedAmount("不添加");
                tip.setAdjustType("忌辣");
                tip.setReason("您忌辣，建议完全去除" + name + "，可用甜椒、彩椒等增加色彩和甜味");
                tips.add(tip);
                found = true;
            }
        }
        if (found) {
            summary.add("忌辣建议：去除所有辣椒、花椒等辣味调料，可用甜椒、彩椒、孜然、五香粉等增添风味");
            for (int i = 0; i < steps.size(); i++) {
                String content = getString(steps.get(i), "content");
                if (content != null && (content.contains("辣") || content.contains("爆香") || content.contains("呛"))) {
                    FlavorAdjustmentDTO.CookingSuggestion sugg = new FlavorAdjustmentDTO.CookingSuggestion();
                    sugg.setStepIndex(String.valueOf(i + 1));
                    sugg.setOriginalContent(content);
                    sugg.setSuggestedContent(content.replace("干辣椒和花椒", "姜片和葱段").replace("爆香", "炒香").replace("呛出辣味", "炒出香味"));
                    sugg.setAdjustType("忌辣烹饪");
                    sugg.setReason("忌辣时去除辣味调料，可用葱姜蒜、孜然、五香粉等代替增香");
                    suggestions.add(sugg);
                }
            }
        }
    }

    private void handleLightSpicy(List<Map<String, Object>> ingredients, List<Map<String, Object>> steps,
                                  List<FlavorAdjustmentDTO.SeasoningTip> tips,
                                  List<FlavorAdjustmentDTO.CookingSuggestion> suggestions,
                                  List<String> summary) {
        boolean found = false;
        for (Map<String, Object> ing : ingredients) {
            String name = getString(ing, "name");
            String amount = getString(ing, "amount");
            if (name != null && containsAny(name, SPICY_KEYWORDS)) {
                FlavorAdjustmentDTO.SeasoningTip tip = new FlavorAdjustmentDTO.SeasoningTip();
                tip.setSeasoningName(name);
                tip.setOriginalAmount(amount != null ? amount : "适量");
                tip.setSuggestedAmount(reduceAmount(amount, 0.5));
                tip.setAdjustType("微辣");
                tip.setReason("您偏好微辣，建议将辣味调料用量减少一半");
                tips.add(tip);
                found = true;
            }
        }
        if (found) {
            summary.add("微辣建议：辣味调料用量减半，既能品尝到香辣味又不会过于刺激");
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String reduceAmount(String amount, double ratio) {
        if (amount == null || amount.isEmpty()) {
            return "少量";
        }
        Matcher m = AMOUNT_PATTERN.matcher(amount);
        if (m.find()) {
            try {
                double num = Double.parseDouble(m.group(1));
                double reduced = num * ratio;
                String unit = m.group(2);
                if (unit == null || unit.isEmpty()) {
                    return String.format("%.1f", reduced);
                }
                if ("少许".equals(unit) || "适量".equals(unit) || "少量".equals(unit)) {
                    return "少量";
                }
                if (reduced < 0.3) {
                    return "少许";
                }
                return String.format("%.1f%s", reduced, unit);
            } catch (Exception e) {
                return "少量";
            }
        }
        if (amount.contains("适量") || amount.contains("少许")) {
            return "少量";
        }
        return "减少用量";
    }
}
