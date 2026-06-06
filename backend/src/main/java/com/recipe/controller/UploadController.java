package com.recipe.controller;

import com.recipe.common.Result;
import com.recipe.util.ImageCompressor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final ImageCompressor imageCompressor;

    @Value("${recipe.image.path:/app/images}")
    private String imagePath;

    @Value("${recipe.image.url-prefix:http://localhost:9025/api/images}")
    private String imageUrlPrefix;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        try {
            String fileName = imageCompressor.compressAndSave(file, imagePath);
            String imageUrl = imageUrlPrefix + "/" + fileName;
            
            Map<String, String> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("url", imageUrl);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }
}