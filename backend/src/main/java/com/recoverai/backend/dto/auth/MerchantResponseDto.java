package com.recoverai.backend.dto.auth;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

public class MerchantResponseDto {

    private UUID id;
    private String name;
    private String email;
    private String razorpayAccountId;
    private MerchantStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public MerchantResponseDto() {
    }

    public MerchantResponseDto(UUID id, String name, String email, String razorpayAccountId,
                               MerchantStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.razorpayAccountId = razorpayAccountId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MerchantResponseDto fromEntity(Merchant merchant) {
        if (merchant == null) {
            return null;
        }
        return new MerchantResponseDto(
                merchant.getId(),
                merchant.getName(),
                merchant.getEmail(),
                merchant.getRazorpayAccountId(),
                merchant.getStatus(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRazorpayAccountId() {
        return razorpayAccountId;
    }

    public void setRazorpayAccountId(String razorpayAccountId) {
        this.razorpayAccountId = razorpayAccountId;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public void setStatus(MerchantStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
