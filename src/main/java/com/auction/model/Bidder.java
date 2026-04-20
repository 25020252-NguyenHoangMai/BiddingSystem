package com.auction.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private boolean sellerEnabled;
    private double balance;
    public Bidder() {
        super();
        this.sellerEnabled = false;
    }

    public Bidder(String id, String username, String password, String fullName, double balance) {
        super(id, username, password, fullName);
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isSellerEnabled() {
        return sellerEnabled;
    }

    public void setSellerEnabled(boolean sellerEnabled) {
        this.sellerEnabled = sellerEnabled;
    }
}
