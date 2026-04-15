package com.auction.exception;

public class AuthenticationException extends AuctionException {
    public AuthenticationException(String message) {
        super(message);
    }
}
//Lỗi sai tài khoản / mật khẩu
//AuthenticationException: xác thực