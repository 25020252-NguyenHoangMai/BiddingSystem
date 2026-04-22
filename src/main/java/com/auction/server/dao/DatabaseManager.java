package com.auction.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static volatile DatabaseManager instance;

    private static final String SERVER_NAME = "localhost\\SQLEXPRESS";
    private static final String DB_NAME = "BiddingSystem";
    private static final String USER = "sa";
    private static final String PASS = "88888888";

    private static final String CONNECTION_URL =
            "jdbc:sqlserver://" + SERVER_NAME + ":1433;" +
                    "databaseName=" + DB_NAME + ";" +
                    "encrypt=true;trustServerCertificate=true;";

    private DatabaseManager() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
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
        return DriverManager.getConnection(CONNECTION_URL, USER, PASS);
    }
}
