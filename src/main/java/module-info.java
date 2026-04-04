module BiddingSystem {
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;

    opens com.auction.client to javafx.graphics, javafx.fxml;

    opens com.auction.client.controller to javafx.fxml;
    opens com.auction.client.util to javafx.graphics, javafx.fxml;

    exports com.auction.client;
}