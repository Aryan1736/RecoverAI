package com.recoverai.backend.dto.auth;

public class AuthResponseDto {

    private String token;
    private String tokenType = "Bearer";
    private long expiresInMs;
    private MerchantResponseDto merchant;

    public AuthResponseDto() {
    }

    public AuthResponseDto(String token, String tokenType, long expiresInMs, MerchantResponseDto merchant) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.expiresInMs = expiresInMs;
        this.merchant = merchant;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public MerchantResponseDto getMerchant() {
        return merchant;
    }

    public void setMerchant(MerchantResponseDto merchant) {
        this.merchant = merchant;
    }
}
