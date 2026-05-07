package com.auction.model;


public class Bidder extends User {

    private static final long serialVersionUID = 1L;

    private boolean sellerEnabled;
    private double balance;
    private double reservedBalance;


    public Bidder() {
        super();
    }


    public Bidder(String id, String username, String password, String fullName, String role, double balance) {
        this(id, username, password, fullName, role, balance, 0.0);
    }


    public Bidder(String id, String username, String password, String fullName,
                  String role, double balance, double reservedBalance) {

        super(id, username, password, fullName, role);

        //bidder tự quản lý tiền của mình
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.sellerEnabled = false;
    }


    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }


    public double getReservedBalance() {
        return reservedBalance;
    }

    public void setReservedBalance(double reservedBalance) {
        this.reservedBalance = reservedBalance;
    }

    //số dư khả dụng
    public double getAvailableBalance() {
        return this.balance - this.reservedBalance;
    }

    public boolean isSellerEnabled() {
        return sellerEnabled;
    }

    public void enableSelling() {
        this.sellerEnabled = true;
    }

    public void disableSelling() {
        this.sellerEnabled = false;
    }

    public void setSellerEnabled(boolean sellerEnabled) {
        this.sellerEnabled = sellerEnabled;
    }

    public String getUserDetails() {
        return "Bidder [" +
                "Username=" + getUsername() +
                ", FullName=" + getFullName() +
                ", TotalBalance=" + balance +
                ", ReservedBalance=" + reservedBalance +
                ", AvailableBalance=" + getAvailableBalance() +
                ", isSeller=" + sellerEnabled +
                ']';
    }
}