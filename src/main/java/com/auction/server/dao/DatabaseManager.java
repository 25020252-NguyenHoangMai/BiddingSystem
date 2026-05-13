package com.auction.server.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static volatile DatabaseManager instance;
    private DataSource dataSource;

    private DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASS"));

        config.setMaximumPoolSize(10);//max là 10 ống kết nối
        config.setMinimumIdle(2);//2 ống luôn mở sẵn

        dataSource = new HikariDataSource(config);
    }

    // Constructor để inject DataSource (dùng cho test)
    private DatabaseManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Phương thức cho test set instance với DB giả
    public static void setTestInstance(DataSource dataSource) {
        instance = new DatabaseManager(dataSource);
    }

    public static void resetInstance() {
        instance = null;
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
