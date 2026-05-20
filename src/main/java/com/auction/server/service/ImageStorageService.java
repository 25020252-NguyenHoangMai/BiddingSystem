package com.auction.server.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class ImageStorageService {
    private static final String UPLOAD_DIR =
            System.getProperty("user.dir") + File.separator +
                    "uploads" + File.separator +
                    "items" + File.separator;
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    public String saveItemImage(byte[] imageBytes, String originalFileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        if (imageBytes.length > MAX_SIZE) {
            throw new IllegalArgumentException("Image size must be <= 5MB");
        }

        String extension = getExtension(originalFileName);

        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("Only jpg, jpeg, png, webp images are allowed");
        }

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String newFileName = UUID.randomUUID() + extension;
        File outputFile = new File(dir, newFileName);

        Files.write(outputFile.toPath(), imageBytes);

        return UPLOAD_DIR + newFileName;
    }

    public byte[] readImage(String imagePath) throws IOException {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        File file = new File(imagePath);

        if (!file.exists()) {
            return null;
        }

        return Files.readAllBytes(file.toPath());
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    private boolean isAllowedExtension(String extension) {
        return extension.equals(".jpg")
                || extension.equals(".jpeg")
                || extension.equals(".png")
                || extension.equals(".webp");
    }
}