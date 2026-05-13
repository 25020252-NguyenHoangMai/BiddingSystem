package com.auction.server.dao;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseDAOTest {

    @BeforeEach
    void setupH2() throws SQLException {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MSSQLServer");
        ds.setUser("sa");
        ds.setPassword("");

        DatabaseManager.setTestInstance(ds);

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Tạo bảng Users (Chứa thông tin Bidder/Seller và số dư)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id VARCHAR(50) PRIMARY KEY, username VARCHAR(50), password VARCHAR(50), full_name VARCHAR(100), role VARCHAR(20), available_balance DOUBLE, reserved_balance DOUBLE)");

            // 2. Tạo bảng Items
            stmt.execute("CREATE TABLE IF NOT EXISTS items (id VARCHAR(50) PRIMARY KEY, seller_id VARCHAR(50), name VARCHAR(100))");

            // 3. Tạo bảng Auction Sessions
            stmt.execute("CREATE TABLE IF NOT EXISTS auction_sessions (id VARCHAR(50) PRIMARY KEY, item_id VARCHAR(50), current_price DOUBLE, current_winner_id VARCHAR(50), status VARCHAR(20), start_time TIMESTAMP, end_time TIMESTAMP)");

            // 4. Tạo bảng Bid Transactions
            stmt.execute("CREATE TABLE IF NOT EXISTS bid_transactions (id VARCHAR(50) PRIMARY KEY, session_id VARCHAR(50), bidder_id VARCHAR(50), bid_amount DOUBLE, created_at TIMESTAMP)");
        }
    }

    @AfterEach
    void cleanupH2() {
        DatabaseManager.resetInstance();
    }
}