package com.auction.server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageStorageServiceTest {

    private ImageStorageService imageStorageService;
    private List<String> createdFiles;

    @BeforeEach
    void setUp() {
        imageStorageService = new ImageStorageService();
        createdFiles = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        for (String path : createdFiles) {
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    @Nested
    class SaveItemImageTests {

        @Test
        void nullOrEmptyBytes_ReturnsNull() throws IOException {
            assertNull(imageStorageService.saveItemImage(null, "test.png"));
            assertNull(imageStorageService.saveItemImage(new byte[0], "test.png"));
        }

        @Test
        void sizeExceedsMax_ThrowsException() {
            byte[] largeImage = new byte[5 * 1024 * 1024 + 1];

            assertThrows(IllegalArgumentException.class,
                    () -> imageStorageService.saveItemImage(largeImage, "test.png"));
        }

        @Test
        void invalidExtensions_ThrowsException() {
            byte[] validBytes = new byte[10];

            assertThrows(IllegalArgumentException.class, () -> imageStorageService.saveItemImage(validBytes, "test.txt"));
            assertThrows(IllegalArgumentException.class, () -> imageStorageService.saveItemImage(validBytes, "test"));
            assertThrows(IllegalArgumentException.class, () -> imageStorageService.saveItemImage(validBytes, null));
        }

        @Test
        void validImage_SavesSuccessfully() throws IOException {
            byte[] validBytes = "dummy_image_data".getBytes();

            String path = imageStorageService.saveItemImage(validBytes, "test.JPG");
            createdFiles.add(path);

            assertNotNull(path);
            assertTrue(path.endsWith(".jpg"));

            File savedFile = new File(path);
            assertTrue(savedFile.exists());
            assertArrayEquals(validBytes, Files.readAllBytes(savedFile.toPath()));
        }
    }

    @Nested
    class ReadImageTests {

        @Test
        void nullOrBlankPath_ReturnsNull() throws IOException {
            assertNull(imageStorageService.readImage(null));
            assertNull(imageStorageService.readImage("   "));
        }

        @Test
        void pathTraversalAttack_ThrowsSecurityException() {
            String maliciousPath = System.getProperty("user.dir") + File.separator + ".." + File.separator + "secret.txt";

            assertThrows(SecurityException.class, () -> imageStorageService.readImage(maliciousPath));
        }

        @Test
        void fileDoesNotExist_ReturnsNull() throws IOException {
            String fakePath = System.getProperty("user.dir") + File.separator +
                    "uploads" + File.separator + "items" + File.separator + "fake_image.png";

            assertNull(imageStorageService.readImage(fakePath));
        }

        @Test
        void validPath_ReturnsBytes() throws IOException {
            byte[] originalBytes = "test_read_data".getBytes();
            String savedPath = imageStorageService.saveItemImage(originalBytes, "test.png");
            createdFiles.add(savedPath);

            byte[] readBytes = imageStorageService.readImage(savedPath);

            assertNotNull(readBytes);
            assertArrayEquals(originalBytes, readBytes);
        }
    }
}