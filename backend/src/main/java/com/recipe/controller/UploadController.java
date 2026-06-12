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
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final ImageCompressor imageCompressor;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Value("${recipe.image.path:/app/images}")
    private String imagePath;

    @Value("${recipe.image.url-prefix:http://localhost:9025/api/images}")
    private String imageUrlPrefix;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("收到图片上传请求, 原始文件名: {}, 大小: {} bytes", originalFilename, file.getSize());

        if (file.isEmpty()) {
            log.warn("上传文件为空");
            return Result.error("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件大小超过限制: {} bytes", file.getSize());
            return Result.error("文件大小不能超过10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("不支持的文件类型: {}", contentType);
            return Result.error("只支持JPG、PNG、GIF、WebP、BMP格式的图片");
        }

        try {
            String fileName = imageCompressor.compressAndSave(file, imagePath);
            String imageUrl = imageUrlPrefix + "/" + fileName;

            Map<String, String> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("url", imageUrl);

            log.info("图片上传成功, 文件名: {}, URL: {}", fileName, imageUrl);
            return Result.success(result);
        } catch (Exception e) {
            log.error("图片上传失败, 原始文件名: {}", originalFilename, e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }
}
