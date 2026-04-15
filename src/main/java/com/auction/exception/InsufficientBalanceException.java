package com.auction.exception;

public class InsufficientBalanceException extends AuctionException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
//Lỗi không đủ tiền đặt cược
//Insufficient Balance : Không đủ số dư