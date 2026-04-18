package com.auction.model;

import java.io.Serializable;

public class Bidder extends User implements Serializable {
    private static final long serialVersionUID = 1L;
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
