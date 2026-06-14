package com.recipe.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserPreferenceDTO {

    private Long userId;

    private List<String> allergies;

    private List<String> dietaryRestrictions;

    private List<String> dislikes;

    private List<String> preferences;

    private String spiceLevel;

    private String tastePreference;
}
