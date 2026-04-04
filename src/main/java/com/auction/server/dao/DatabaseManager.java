package com.auction.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String SERVER_NAME = "localhost";
    private static final String DB_NAME = "BiddingSystem";
    private static final String USER = "sa";
    private static final String PASS = "141414";

    private static final String CONNECTION_URL =
            "jdbc:sqlserver://" + SERVER_NAME + ":1433;" +
                    "databaseName=" + DB_NAME + ";" +
                    "encrypt=true;trustServerCertificate=true;";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(CONNECTION_URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            throw new SQLException(e);
        }
    }
}
