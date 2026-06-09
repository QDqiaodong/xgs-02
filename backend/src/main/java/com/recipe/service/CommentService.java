package com.recipe.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.recipe.entity.Comment;

public interface CommentService {

    Comment addComment(Long recipeId, String content);

    IPage<Comment> getCommentsByRecipeId(Long recipeId, Integer page, Integer size);

    boolean deleteComment(Long id);

    Comment getCommentById(Long id);
}
