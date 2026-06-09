package com.recipe.dto;

import lombok.Data;

@Data
public class UserStatsDTO {

    private Long userId;

    private String nickname;

    private String avatar;

    private Integer recipeCount;

    private Integer favoriteCount;

    private Integer totalLikes;

    private Integer viewCount;

    private Integer draftCount;
}
