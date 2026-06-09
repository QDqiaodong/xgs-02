package com.recipe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recipe.entity.Comment;
import com.recipe.mapper.CommentMapper;
import com.recipe.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USERNAME = "美食爱好者";

    @Override
    @Transactional
    public Comment addComment(Long recipeId, String content) {
        if (recipeId == null || content == null || content.trim().isEmpty()) {
            return null;
        }

        Comment comment = new Comment();
        comment.setUserId(DEFAULT_USER_ID);
        comment.setUsername(DEFAULT_USERNAME);
        comment.setRecipeId(recipeId);
        comment.setContent(content.trim());

        int inserted = commentMapper.insert(comment);
        if (inserted <= 0) {
            log.warn("插入评论失败，recipeId={}", recipeId);
            return null;
        }

        return commentMapper.selectById(comment.getId());
    }

    @Override
    public IPage<Comment> getCommentsByRecipeId(Long recipeId, Integer page, Integer size) {
        if (recipeId == null) {
            return new Page<>();
        }

        int current = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 10;

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRecipeId, recipeId)
                .eq(Comment::getDeleted, 0)
                .orderByDesc(Comment::getCreatedAt);

        return commentMapper.selectPage(new Page<>(current, pageSize), wrapper);
    }

    @Override
    @Transactional
    public boolean deleteComment(Long id) {
        if (id == null) {
            return false;
        }

        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            return false;
        }

        if (!DEFAULT_USER_ID.equals(comment.getUserId())) {
            log.warn("无权限删除评论，commentId={}, userId={}", id, comment.getUserId());
            return false;
        }

        int deleted = commentMapper.deleteById(id);
        return deleted > 0;
    }

    @Override
    public Comment getCommentById(Long id) {
        if (id == null) {
            return null;
        }
        return commentMapper.selectById(id);
    }
}
