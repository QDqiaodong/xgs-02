package com.recipe.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Component
public class ImageCompressor {

    public String compressAndSave(MultipartFile file, String uploadPath) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        String fileName = UUID.randomUUID().toString() + "." + extension;
        
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        File outputFile = new File(uploadPath, fileName);
        
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) {
                file.transferTo(outputFile);
                return fileName;
            }
            
            float quality = 0.8f;
            long fileSize = file.getSize();
            if (fileSize > 2 * 1024 * 1024) {
                quality = 0.6f;
            } else if (fileSize > 1 * 1024 * 1024) {
                quality = 0.7f;
            }
            
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(extension);
            if (!writers.hasNext()) {
                file.transferTo(outputFile);
                return fileName;
            }
            
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
        }
        
        return fileName;
    }
}