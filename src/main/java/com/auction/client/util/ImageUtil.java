package com.auction.client.util;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.nio.file.*;
import java.util.function.Consumer;

public class ImageUtil {
    private static final Path AVATAR_DIR = Paths.get(System.getProperty("user.home"), ".bidding", "avatars");
    private static final String[] EXTENSIONS = {".png", ".jpg", ".jpeg"};

    public static void loadAvatar(String userId, String username, ImageView imgView, Label lblPlaceholder) {
        try {
            for (String ext : EXTENSIONS) {
                Path path = AVATAR_DIR.resolve(userId + ext);
                if (Files.exists(path)) {
                    imgView.setImage(new Image(path.toUri().toString(), 100, 100, false, true));
                    imgView.setVisible(true);

                    // Chỉ ẩn placeholder nếu đối tượng label này tồn tại
                    if (lblPlaceholder != null) {
                        lblPlaceholder.setVisible(false);
                    }
                    return;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // Nếu không có file ảnh -> Hiện placeholder xám theo tên người dùng
        imgView.setVisible(false);

        // Chỉ hiển thị và gán chữ nếu lblPlaceholder khác null
        if (lblPlaceholder != null) {
            lblPlaceholder.setVisible(true);
            lblPlaceholder.setText(
                    (username != null && !username.isEmpty()) ? username.substring(0, 1).toUpperCase() : "?"
            );
        }
    }

    public static void saveAvatarAsync(String userId, File file, Consumer<Path> onDone) {
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                Files.createDirectories(AVATAR_DIR);
                for (String ext : EXTENSIONS) Files.deleteIfExists(AVATAR_DIR.resolve(userId + ext));

                String ext = file.getName().substring(file.getName().lastIndexOf('.'));
                Path dest = AVATAR_DIR.resolve(userId + ext);
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                return dest;
            }
        };
        task.setOnSucceeded(e -> onDone.accept(task.getValue()));
        new Thread(task).start();
    }
}