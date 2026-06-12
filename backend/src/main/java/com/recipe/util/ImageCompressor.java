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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class ImageCompressor {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_RETRY = 5;

    public String compressAndSave(MultipartFile file, String uploadPath) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        String dateDir = LocalDate.now().format(DATE_FORMATTER);
        Path fullUploadDir = Paths.get(uploadPath, dateDir);
        if (!Files.exists(fullUploadDir)) {
            Files.createDirectories(fullUploadDir);
        }

        String fileName = generateUniqueFileName(extension, fullUploadDir.toString());
        File outputFile = new File(fullUploadDir.toFile(), fileName);

        File tempFile = new File(fullUploadDir.toFile(), fileName + ".tmp");

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                file.transferTo(tempFile);
                tempFile.renameTo(outputFile);
                return dateDir + "/" + fileName;
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
                file.transferTo(tempFile);
                tempFile.renameTo(outputFile);
                return dateDir + "/" + fileName;
            }

            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();

            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(tempFile)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }

            if (!tempFile.renameTo(outputFile)) {
                throw new IOException("Failed to rename temp file to final file");
            }
        } finally {
            if (tempFile.exists()) {
                try {
                    tempFile.delete();
                } catch (Exception e) {
                    log.warn("Failed to delete temp file: {}", tempFile.getPath(), e);
                }
            }
        }

        return dateDir + "/" + fileName;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "jpg";
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "jpg";
        }
        String ext = originalFilename.substring(dotIndex + 1).toLowerCase();
        if (ext.isEmpty()) {
            return "jpg";
        }
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }
        return ext;
    }

    private String generateUniqueFileName(String extension, String directory) {
        for (int i = 0; i < MAX_RETRY; i++) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            int random = ThreadLocalRandom.current().nextInt(1000, 9999);
            String fileName = timestamp + "_" + uuid + "_" + random + "." + extension;

            File file = new File(directory, fileName);
            if (!file.exists()) {
                return fileName;
            }
            log.warn("File name conflict detected, retrying: {}", fileName);
        }
        throw new RuntimeException("Failed to generate unique file name after " + MAX_RETRY + " retries");
    }
}
