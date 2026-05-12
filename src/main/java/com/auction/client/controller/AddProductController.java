package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.service.ProductService;
import com.auction.dto.ItemDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class AddProductController {

    @FXML private TextField txtName, txtPrice, txtDuration;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cbCategory;
    @FXML private VBox dynamicFields;
    @FXML private Label errorLabel;
    @FXML private Button btnSubmit;

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
        cbCategory.getItems().addAll(categoryFields.keySet());
        // Giá trị duration mặc định 24 giờ
        txtDuration.setText("24");
    }

    @FXML
    private void handleCategoryChange(ActionEvent event) {
        dynamicFields.getChildren().clear();
        fields.clear();
        dynamicFields.setVisible(false);
        dynamicFields.setManaged(false);

        String cat = cbCategory.getValue();
        if (cat == null) return;

        String[] fieldNames = categoryFields.get(cat);
        if (fieldNames != null) addFields(fieldNames);
        dynamicFields.setVisible(true);
        dynamicFields.setManaged(true);
    }

    private void addFields(String... names) {
        for (String n : names) {
            TextField tf = new TextField();
            tf.setPromptText("Nhập " + n.toLowerCase());

            dynamicFields.getChildren().addAll(new Label(n + ":"), tf);
            fields.put(n, tf);
        }
    }

    @FXML
    private void handleAddAndStart(ActionEvent event) {
        if (submitting) {
            return;
        }

        submitting = true;
        btnSubmit.setDisable(true);
        btnSubmit.setText("Đang xử lý...");
        errorLabel.setVisible(false);

        try {
            // 1. Build đối tượng từ form UI
            ItemDTO item = buildItem();

            // 2. Gọi service (nếu thất bại, service sẽ tự throw RuntimeException)
            ItemDTO savedItem = service.addProduct(item);

            // 3. Thông báo thành công
            if (savedItem != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Đã thêm sản phẩm '" + savedItem.getName() + "' thành công!");
                alert.showAndWait();
                close(); // Đóng window hiện tại
            }

        } catch (NumberFormatException e) {
            errorLabel.setText("Giá hoặc Mileage phải là số hợp lệ!");
            errorLabel.setVisible(true);
            submitting = false;
            btnSubmit.setDisable(false);
            btnSubmit.setText("Thêm sản phẩm");
        } catch (Exception e) {
            // Hiển thị mọi lỗi từ kết nối, logic server,... lên UI
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
            submitting = false;
            btnSubmit.setDisable(false);
            btnSubmit.setText("Thêm sản phẩm");
        }
    }

    // Tách riêng logic build + validate
    private ItemDTO buildItem() throws Exception {

        var user = ClientSession.getCurrentUser();

        if (user == null)
            throw new Exception("Bạn chưa đăng nhập!");

        if (txtName.getText().isBlank())
            throw new Exception("Tên sản phẩm không được để trống!");

        if (cbCategory.getValue() == null)
            throw new Exception("Vui lòng chọn danh mục!");

        double price = Double.parseDouble(txtPrice.getText());
        if (price <= 0)
            throw new Exception("Giá khởi điểm phải lớn hơn 0");

        // ===== DURATION =====
        String durText = txtDuration.getText().trim();
        if (durText.isBlank()) throw new Exception("Vui lòng nhập thời gian đấu giá!");

        int durationHours;
        try {
            durationHours = Integer.parseInt(durText);
        } catch (NumberFormatException e) {
            throw new Exception("Thời gian phải là số nguyên (giờ)!");
        }
        if (durationHours < 1 || durationHours > 720)
            throw new Exception("Thời gian đấu giá phải từ 1 đến 720 giờ!");

        // Tính endTimeMillis = bây giờ + durationHours
        long endTimeMillis = System.currentTimeMillis() + (long) durationHours * 3_600_000L;

        ItemDTO item = new ItemDTO();
        item.setName(txtName.getText().trim());
        item.setStartingPrice(price);
        item.setSellerId(user.getId());
        item.setItemType(cbCategory.getValue().toUpperCase());
        item.setDescription(txtDescription.getText().trim());
        item.setEndTimeMillis(endTimeMillis);   // Truyền thời gian kết thúc

        // Xử lý dynamic fields
        for (var entry : fields.entrySet()) {
            String key = entry.getKey();
            TextField field = entry.getValue();

            String val = field.getText().trim();

            if (val.isEmpty()) {
                field.requestFocus();
                throw new Exception("Vui lòng nhập " + key);
            }

            mapField(item, key, val);
        }

        return item;
    }

    private void mapField(ItemDTO item, String key, String val) throws Exception {
        switch (key) {
            case "Mileage" -> {
                int m = Integer.parseInt(val);
                if (m < 0) throw new Exception("Mileage không được âm");
                item.setMileage(m);
            }
            case "Model" -> item.setModel(val);
            case "EngineType" -> item.setEngineType(val);
            case "Brand" -> item.setBrand(val);
            case "Artist" -> item.setArtist(val);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        close();
    }

    private void close() {
        ((Stage) btnSubmit.getScene().getWindow()).close();
    }
}