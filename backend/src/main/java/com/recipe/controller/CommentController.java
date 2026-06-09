package com.recipe.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.recipe.common.Result;
import com.recipe.entity.Comment;
import com.recipe.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public Result<Comment> addComment(@RequestBody Map<String, Object> body) {
        Long recipeId = body.get("recipeId") != null ? Long.valueOf(body.get("recipeId").toString()) : null;
        String content = body.get("content") != null ? body.get("content").toString() : null;

        Comment comment = commentService.addComment(recipeId, content);
        if (comment == null) {
            return Result.error("评论发布失败");
        }
        return Result.success(comment);
    }

    @GetMapping
    public Result<IPage<Comment>> getComments(
            @RequestParam Long recipeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(commentService.getCommentsByRecipeId(recipeId, page, size));
    }

    @GetMapping("/{id}")
    public Result<Comment> getComment(@PathVariable Long id) {
        Comment comment = commentService.getCommentById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        return Result.success(comment);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id) {
        boolean success = commentService.deleteComment(id);
        if (!success) {
            return Result.error("删除失败，无权限或评论不存在");
        }
        return Result.success(true);
    }
}
