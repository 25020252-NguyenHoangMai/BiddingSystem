package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.service.ProductService;
import com.auction.dto.ItemDTO;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.*;

public class AddProduct2Controller {

    @FXML private TextField txtName, txtPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cbCategory;
    @FXML private VBox dynamicFields;
    @FXML private Label errorLabel;
    @FXML private Button btnSubmit;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private Spinner<Integer> spinStartHour, spinStartMin, spinStartSec;
    @FXML private Spinner<Integer> spinEndHour, spinEndMin, spinEndSec;
    @FXML private ImageView productPreviewImage;
    private File selectedImageFile;

    private final Map<String, TextField> fields = new LinkedHashMap<>();
    private final ProductService service = ProductService.getInstance();
    private volatile boolean submitting;

    private final Map<String, String[]> categoryFields = Map.of(
            "Vehicle", new String[]{"Model", "EngineType", "Mileage"},
            "Electronics", new String[]{"Brand"},
            "Art", new String[]{"Artist"}
    );

    @FXML
    public void initialize() {
        // Setup ComboBox
        cbCategory.getItems().addAll(categoryFields.keySet());

        // Setup Spinners : Giá trị từ 0-23 cho giờ, 0-59 cho phút/giây
        setupTimeSpinner(spinStartHour, 23);
        setupTimeSpinner(spinStartMin, 59);
        setupTimeSpinner(spinStartSec, 59);
        setupTimeSpinner(spinEndHour, 23);
        setupTimeSpinner(spinEndMin, 59);
        setupTimeSpinner(spinEndSec, 59);

        // Đặt giá trị mặc định (Bắt đầu: ngay bây giờ, Kết thúc: sau 24h)
        LocalDateTime now = LocalDateTime.now();
        dpStartDate.setValue(now.toLocalDate());
        spinStartHour.getValueFactory().setValue(now.getHour());
        spinStartMin.getValueFactory().setValue(now.getMinute());

        LocalDateTime tomorrow = now.plusDays(1);
        dpEndDate.setValue(tomorrow.toLocalDate());
        spinEndHour.getValueFactory().setValue(tomorrow.getHour());
        spinEndMin.getValueFactory().setValue(tomorrow.getMinute());
    }

    private void setupTimeSpinner(Spinner<Integer> spinner, int max) {
        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, 0);
        spinner.setValueFactory(factory);
    }

    @FXML
    private void handleCategoryChange(ActionEvent event) {
        dynamicFields.getChildren().clear();
        fields.clear();

        String cat = cbCategory.getValue();
        if (cat == null) {
            dynamicFields.setVisible(false);
            dynamicFields.setManaged(false);
            return;
        }

        String[] fieldNames = categoryFields.get(cat);
        if (fieldNames != null) {
            for (String n : fieldNames) {
                Label label = new Label(n + ":");
                label.setStyle("-fx-font-weight: bold;");
                TextField tf = new TextField();
                tf.setPromptText("Enter " + n.toLowerCase());

                dynamicFields.getChildren().addAll(label, tf);
                fields.put(n, tf);
            }
            dynamicFields.setVisible(true);
            dynamicFields.setManaged(true);
        }
        else {
            dynamicFields.setVisible(false);
            dynamicFields.setManaged(false);
        }
    }

    @FXML
    private void handleAddAndStart(ActionEvent event) {
        if (submitting) return;

        ItemDTO item;
        try {
            item = buildItem();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
            return;
        }

        submitting = true;
        btnSubmit.setDisable(true);
        btnSubmit.setText("Processing...");
        errorLabel.setVisible(false);

        Task<ItemDTO> task = new Task<>() {
            @Override
            protected ItemDTO call() throws Exception {
                return service.addProduct(item);
            }
        };

        task.setOnSucceeded(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Product '" + task.getValue().getName() + "' posted!");
            alert.showAndWait();
            close();
        });

        task.setOnFailed(e -> {
            submitting = false;
            btnSubmit.setDisable(false);
            btnSubmit.setText("Post Auction");
            errorLabel.setText(task.getException().getMessage());
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    private ItemDTO buildItem() throws Exception {
        var user = ClientSession.getCurrentUser();
        if (user == null) throw new Exception("Please login first!");

        if (txtName.getText().isBlank()) throw new Exception("Name is required!");
        if (cbCategory.getValue() == null) throw new Exception("Category is required!");

        double price;
        try {
            price = Double.parseDouble(txtPrice.getText());
        } catch (NumberFormatException e) {
            throw new Exception("Invalid price format!");
        }

        // Xử lý logic thời gian
        LocalDateTime startDateTime = LocalDateTime.of(dpStartDate.getValue(),
                LocalTime.of(spinStartHour.getValue(), spinStartMin.getValue(), spinStartSec.getValue()));

        LocalDateTime endDateTime = LocalDateTime.of(dpEndDate.getValue(),
                LocalTime.of(spinEndHour.getValue(), spinEndMin.getValue(), spinEndSec.getValue()));

        if (endDateTime.isBefore(startDateTime)) {
            throw new Exception("End time must be after start time!");
        }

        LocalDateTime now = LocalDateTime.now();
        if (startDateTime.isBefore(now.minusMinutes(1))) { // Trừ 1 phút để bù trừ độ trễ thao tác
            throw new Exception("Start time cannot be in the past!");
        }

        long startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        ItemDTO item = new ItemDTO();
        item.setName(txtName.getText().trim());
        item.setStartingPrice(price);
        item.setSellerId(user.getId());
        item.setItemType(cbCategory.getValue().toUpperCase());
        item.setDescription(txtDescription.getText().trim());

        item.setStartTimeMillis(startMillis);
        item.setEndTimeMillis(endMillis);

        for (var entry : fields.entrySet()) {
            String val = entry.getValue().getText().trim();
            if (val.isEmpty()) throw new Exception("Please enter " + entry.getKey());
            mapField(item, entry.getKey(), val);
        }

        if (selectedImageFile != null) {
            byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());

            item.setImageBytes(imageBytes);
            item.setImageFileName(selectedImageFile.getName());
        }

        return item;
    }

    private void mapField(ItemDTO item, String key, String val) throws Exception {
        switch (key) {
            case "Mileage" -> item.setMileage(Integer.parseInt(val));
            case "Model" -> item.setModel(val);
            case "EngineType" -> item.setEngineType(val);
            case "Brand" -> item.setBrand(val);
            case "Artist" -> item.setArtist(val);
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Product Image");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png",
                        "*.jpg",
                        "*.jpeg"
                )
        );

        File file = chooser.showOpenDialog(productPreviewImage.getScene().getWindow());

        if (file != null) {
            selectedImageFile = file;

            Image image = new Image(file.toURI().toString());

            productPreviewImage.setImage(image);
        }
    }

    @FXML private void handleCancel(ActionEvent event) { close(); }

    private void close() {
        ((Stage) btnSubmit.getScene().getWindow()).close();
    }
}