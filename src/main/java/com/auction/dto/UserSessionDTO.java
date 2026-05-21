package com.auction.dto;

import java.io.Serializable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class UserSessionDTO implements Serializable {
    private String id;
    private String username;
    private String fullName;
    private String role;
    private double balance;
    private boolean sellerEnabled;
    private double reservedBalance;
    private String status;



    private transient BooleanProperty selected = new SimpleBooleanProperty(false);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isSellerEnabled() {return sellerEnabled;}

    public void setSellerEnabled(boolean sellerEnabled) {this.sellerEnabled = sellerEnabled;}

    public BooleanProperty selectedProperty() {
        // Nếu null (do vừa bay qua mạng về), thì khởi tạo mới
        if (selected == null) {
            selected = new SimpleBooleanProperty(false);
        }
        return selected;
    }

    public boolean isSelected() {
        if (selected == null) selected = new SimpleBooleanProperty(false);
        return selected.get();
    }

    public void setSelected(boolean value) {
        if (selected == null) selected = new SimpleBooleanProperty(false);
        selected.set(value);
    }

    public double getReservedBalance() { return reservedBalance;}

    public void setReservedBalance(double reservedBalance) { this.reservedBalance = reservedBalance;
     }

    public double getAvailableBalance() {
        return balance - reservedBalance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
