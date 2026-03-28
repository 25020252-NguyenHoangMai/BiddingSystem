package com.auction.server.model;

public class Bidder extends User {
    private double balance;
    public Bidder() {
        super();
    }

    public Bidder(String id, String username, String password, String fullName, double balance) {
        super(id, username, password, fullName, "BIDDER");
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
