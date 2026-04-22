package com.auction.exception;

public class InvalidBidException extends AuctionException {
    public InvalidBidException(String message) {
        super(message);
    }
}
//giá k hợp lệ vd trả giá nhỏ hơn