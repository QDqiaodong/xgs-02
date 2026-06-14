package com.recipe.service;

import com.recipe.dto.FlavorAdjustmentDTO;
import com.recipe.entity.FamilyTasteProfile;

import java.util.List;

public interface FamilyTasteProfileService {

    List<FamilyTasteProfile> getProfiles();

    FamilyTasteProfile getDefaultProfile();

    FamilyTasteProfile getProfileById(Long id);

    FamilyTasteProfile createProfile(FamilyTasteProfile profile);

    FamilyTasteProfile updateProfile(Long id, FamilyTasteProfile profile);

    boolean deleteProfile(Long id);

    boolean setDefaultProfile(Long id);

    FlavorAdjustmentDTO getFlavorAdjustment(Long recipeId);

    FlavorAdjustmentDTO getFlavorAdjustmentByProfile(Long recipeId, Long profileId);
}
