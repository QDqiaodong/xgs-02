package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.dto.FlavorAdjustmentDTO;
import com.recipe.entity.FamilyTasteProfile;
import com.recipe.service.FamilyTasteProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/taste-profile")
@RequiredArgsConstructor
public class FamilyTasteProfileController {

    private final FamilyTasteProfileService tasteProfileService;

    @GetMapping
    public Result<List<FamilyTasteProfile>> getProfiles() {
        return Result.success(tasteProfileService.getProfiles());
    }

    @GetMapping("/default")
    public Result<FamilyTasteProfile> getDefaultProfile() {
        return Result.success(tasteProfileService.getDefaultProfile());
    }

    @GetMapping("/{id}")
    public Result<FamilyTasteProfile> getProfileById(@PathVariable Long id) {
        return Result.success(tasteProfileService.getProfileById(id));
    }

    @PostMapping
    public Result<FamilyTasteProfile> createProfile(@RequestBody FamilyTasteProfile profile) {
        return Result.success(tasteProfileService.createProfile(profile));
    }

    @PutMapping("/{id}")
    public Result<FamilyTasteProfile> updateProfile(@PathVariable Long id, @RequestBody FamilyTasteProfile profile) {
        return Result.success(tasteProfileService.updateProfile(id, profile));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteProfile(@PathVariable Long id) {
        return Result.success(tasteProfileService.deleteProfile(id));
    }

    @PutMapping("/{id}/default")
    public Result<Boolean> setDefaultProfile(@PathVariable Long id) {
        return Result.success(tasteProfileService.setDefaultProfile(id));
    }

    @GetMapping("/recipe/{recipeId}/adjustment")
    public Result<FlavorAdjustmentDTO> getFlavorAdjustment(@PathVariable Long recipeId) {
        return Result.success(tasteProfileService.getFlavorAdjustment(recipeId));
    }

    @GetMapping("/recipe/{recipeId}/adjustment/{profileId}")
    public Result<FlavorAdjustmentDTO> getFlavorAdjustmentByProfile(
            @PathVariable Long recipeId,
            @PathVariable Long profileId) {
        return Result.success(tasteProfileService.getFlavorAdjustmentByProfile(recipeId, profileId));
    }
}
