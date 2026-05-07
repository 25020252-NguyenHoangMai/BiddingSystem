package com.auction.server.service;

public class UserBalance {
    private final double balance;
    private final double reservedBalance;

    public  UserBalance(double balance, double reservedBalance) {
        this.balance = balance;
        this.reservedBalance = reservedBalance;
    }

    public double getBalance() {
        return balance;
    }

    public double getReservedBalance() {
        return reservedBalance;
    }

    public double getAvailableBalance() {
        return balance - reservedBalance;
    }
}
