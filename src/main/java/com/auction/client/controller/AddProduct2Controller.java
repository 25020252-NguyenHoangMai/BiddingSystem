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

import java.time.Instant;
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
import java.util.function.Consumer;

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
    private ItemDTO editingItem;
    private boolean editMode;
    private Consumer<ItemDTO> onUpdateSuccess;

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
        rebuildDynamicFields(cbCategory.getValue());
    }

    private void rebuildDynamicFields(String category) {
        dynamicFields.getChildren().clear();
        fields.clear();

        if (category == null) {
            dynamicFields.setVisible(false);
            dynamicFields.setManaged(false);
            return;
        }

        String[] fieldNames = categoryFields.get(category);

        if (fieldNames == null) {
            dynamicFields.setVisible(false);
            dynamicFields.setManaged(false);
            return;
        }

        for (String name : fieldNames) {
            Label label = new Label(name + ":");
            label.setStyle("-fx-font-weight: bold;");

            TextField tf = new TextField();
            tf.setPromptText("Enter " + name.toLowerCase());

            dynamicFields.getChildren().addAll(label, tf);
            fields.put(name, tf);
        }

        dynamicFields.setVisible(true);
        dynamicFields.setManaged(true);
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
                if (!editMode) {
                    return service.addProduct(item);
                }

                if (editingItem == null) {
                    throw new IllegalStateException("Editing item is missing.");
                }

                String status = editingItem.getSessionStatus();

                if ("OPEN".equals(status)) {
                    item.setId(editingItem.getId());
                    item.setSessionId(editingItem.getSessionId());
                    return service.updateProductBySeller(item);
                }

                if ("RUNNING".equals(status)) {
                    return service.updateAuctionEndTimeBySeller(
                            editingItem.getSessionId(),
                            item.getEndTimeMillis()
                    );
                }

                throw new IllegalStateException("This auction cannot be updated.");
            }
        };

        task.setOnSucceeded(e -> {
            ItemDTO saved = task.getValue();
            if (editMode && onUpdateSuccess != null) {
                onUpdateSuccess.accept(saved);
            }
            String message = editMode ? "Product updated!" : "Product posted!";
            new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
            close();
        });

        task.setOnFailed(e -> {
            submitting = false;
            btnSubmit.setDisable(false);
            btnSubmit.setText(editMode ? "Update Auction" : "Post Auction");
            errorLabel.setText(task.getException().getMessage());
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    private ItemDTO buildItem() throws Exception {
        var user = ClientSession.getCurrentUser();
        if (user == null) throw new Exception("Please login first!");

        String status = (editMode && editingItem != null) ? editingItem.getSessionStatus() : null;
        boolean isRunning = "RUNNING".equals(status);


        if (!txtName.isDisabled()) {
            if (txtName.getText().isBlank()) throw new Exception("Name is required!");
        }
        if (!cbCategory.isDisabled()) {
            if (cbCategory.getValue() == null) throw new Exception("Category is required!");
        }

        double price;
        if (!txtPrice.isDisabled()) {
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
                if (price <= 0) throw new Exception("Starting price must be greater than zero!");
            } catch (NumberFormatException e) {
                throw new Exception("Invalid price format!");
            }
        } else {
            // RUNNING: giữ nguyên giá cũ
            price = (editingItem != null) ? editingItem.getStartingPrice() : 0;
        }

        // Xử lý thời gian bắt đầu
        LocalDateTime startDateTime;
        if (!dpStartDate.isDisabled()) {
            startDateTime = LocalDateTime.of(
                    dpStartDate.getValue(),
                    LocalTime.of(spinStartHour.getValue(), spinStartMin.getValue(), spinStartSec.getValue())
            );
            LocalDateTime now = LocalDateTime.now();
            if (startDateTime.isBefore(now.minusMinutes(1))) {
                throw new Exception("Start time cannot be in the past!");
            }
        } else {
            // RUNNING: lấy lại startTime gốc từ editingItem, không validate lại
            startDateTime = (editingItem != null && editingItem.getStartTimeMillis() > 0)
                    ? LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(editingItem.getStartTimeMillis()),
                    ZoneId.systemDefault())
                    : LocalDateTime.now();
        }

        // Xử lý thời gian kết thúc (luôn cho sửa)
        LocalDateTime endDateTime = LocalDateTime.of(
                dpEndDate.getValue(),
                LocalTime.of(spinEndHour.getValue(), spinEndMin.getValue(), spinEndSec.getValue())
        );

        if (!endDateTime.isAfter(startDateTime)) {
            throw new Exception("End time must be after start time!");
        }

        // RUNNING: cho phép rút ngắn hoặc kéo dài thời gian
        if (isRunning) {

            LocalDateTime now = LocalDateTime.now();

            if (!endDateTime.isAfter(now)) {
                throw new Exception("End time must be after current time!");
            }
        }

        long startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis   = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        ItemDTO item = new ItemDTO();

        // Name, description, category: dùng giá trị cũ nếu bị lock
        item.setName(!txtName.isDisabled()
                ? txtName.getText().trim()
                : (editingItem != null ? editingItem.getName() : ""));
        item.setDescription(!txtDescription.isDisabled()
                ? txtDescription.getText().trim()
                : (editingItem != null ? editingItem.getDescription() : ""));
        item.setItemType(!cbCategory.isDisabled()
                ? cbCategory.getValue().toUpperCase()
                : (editingItem != null ? editingItem.getItemType() : ""));

        item.setStartingPrice(price);
        item.setSellerId(user.getId());
        item.setStartTimeMillis(startMillis);
        item.setEndTimeMillis(endMillis);

        // Dynamic fields: chỉ validate khi không bị lock
        for (var entry : fields.entrySet()) {
            TextField tf = entry.getValue();
            String val = tf.getText().trim();
            if (!tf.isDisabled()) {
                if (val.isEmpty()) throw new Exception("Please enter " + entry.getKey());
                mapField(item, entry.getKey(), val);
            } else {
                // Giữ lại giá trị cũ từ editingItem
                mapFieldFromExisting(item, entry.getKey());
            }
        }

        if (selectedImageFile != null) {
            byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());
            item.setImageBytes(imageBytes);
            item.setImageFileName(selectedImageFile.getName());
        }

        if (editMode && editingItem != null) {
            item.setId(editingItem.getId());
            item.setSessionId(editingItem.getSessionId());
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

    private void mapFieldFromExisting(ItemDTO item, String key) {
        if (editingItem == null) return;
        switch (key) {
            case "Model"      -> item.setModel(editingItem.getModel());
            case "EngineType" -> item.setEngineType(editingItem.getEngineType());
            case "Mileage"    -> item.setMileage(editingItem.getMileage());
            case "Brand"      -> item.setBrand(editingItem.getBrand());
            case "Artist"     -> item.setArtist(editingItem.getArtist());
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

    public void setEditingItem(ItemDTO item) {
        if (item == null) {
            return;
        }

        this.editingItem = item;
        this.editMode = true;

        fillFormForEdit(item);
        applyEditPermissions(item);

        btnSubmit.setText("Update Auction");
    }

    private void fillFormForEdit(ItemDTO item) {
        txtName.setText(item.getName() != null ? item.getName() : "");
        txtDescription.setText(item.getDescription() != null ? item.getDescription() : "");
        txtPrice.setText(String.valueOf(item.getStartingPrice()));

        String category = item.getItemType();

        if (category != null && !category.isBlank()) {
            String normalizedCategory = normalizeCategory(category);
            cbCategory.setValue(normalizedCategory);
            rebuildDynamicFields(normalizedCategory);
            fillDynamicFields(item, normalizedCategory);
        }

        if (item.getStartTimeMillis() > 0) {
            setDateTimeControls(
                    item.getStartTimeMillis(),
                    dpStartDate,
                    spinStartHour,
                    spinStartMin,
                    spinStartSec
            );
        }

        if (item.getEndTimeMillis() > 0) {
            setDateTimeControls(
                    item.getEndTimeMillis(),
                    dpEndDate,
                    spinEndHour,
                    spinEndMin,
                    spinEndSec
            );
        }
    }

    private void applyEditPermissions(ItemDTO item) {
        String status = item.getSessionStatus();

        if ("OPEN".equals(status)) {
            txtName.setDisable(false);
            txtDescription.setDisable(false);
            txtPrice.setDisable(false);
            cbCategory.setDisable(false);

            dpStartDate.setDisable(false);
            spinStartHour.setDisable(false);
            spinStartMin.setDisable(false);
            spinStartSec.setDisable(false);

            dpEndDate.setDisable(false);
            spinEndHour.setDisable(false);
            spinEndMin.setDisable(false);
            spinEndSec.setDisable(false);

        } else if ("RUNNING".equals(status)) {
            // RUNNING -> Chỉ cho phép kéo dài endTime (endTime mới > endTime cũ)
            // Khóa các trường thông tin cơ bản và startTime
            txtName.setDisable(true);
            txtDescription.setDisable(true);
            txtPrice.setDisable(true);
            cbCategory.setDisable(true);

            dpStartDate.setDisable(true);
            spinStartHour.setDisable(true);
            spinStartMin.setDisable(true);
            spinStartSec.setDisable(true);

            // Mở cho phép sửa endTime
            dpEndDate.setDisable(false);
            spinEndHour.setDisable(false);
            spinEndMin.setDisable(false);
            spinEndSec.setDisable(false);
        }
    }

    private String normalizeCategory(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return null;
        }

        return switch (itemType.toUpperCase()) {
            case "VEHICLE" -> "Vehicle";
            case "ELECTRONICS" -> "Electronics";
            case "ART" -> "Art";
            default -> itemType;
        };
    }

    private void fillDynamicFields(ItemDTO item, String category) {
        switch (category) {
            case "Vehicle" -> {
                setFieldValue("Model", item.getModel());
                setFieldValue("EngineType", item.getEngineType());
                setFieldValue("Mileage", item.getMileage() > 0 ? String.valueOf(item.getMileage()) : "");
            }
            case "Electronics" -> setFieldValue("Brand", item.getBrand());
            case "Art" -> setFieldValue("Artist", item.getArtist());
            default -> {
            }
        }
    }

    private void setFieldValue(String key, String value) {
        TextField field = fields.get(key);

        if (field != null) {
            field.setText(value != null ? value : "");
        }
    }

    private void setDateTimeControls(
            long millis,
            DatePicker datePicker,
            Spinner<Integer> hourSpinner,
            Spinner<Integer> minuteSpinner,
            Spinner<Integer> secondSpinner
    ) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(millis),
                ZoneId.systemDefault()
        );

        datePicker.setValue(dateTime.toLocalDate());
        hourSpinner.getValueFactory().setValue(dateTime.getHour());
        minuteSpinner.getValueFactory().setValue(dateTime.getMinute());
        secondSpinner.getValueFactory().setValue(dateTime.getSecond());
    }

    public void setOnUpdateSuccess(java.util.function.Consumer<ItemDTO> callback) {
        this.onUpdateSuccess = callback;
    }

    @FXML private void handleCancel(ActionEvent event) { close(); }

    private void close() {
        ((Stage) btnSubmit.getScene().getWindow()).close();
    }
}