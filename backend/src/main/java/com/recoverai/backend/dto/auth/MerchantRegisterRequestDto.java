package com.recoverai.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MerchantRegisterRequestDto {

    @NotBlank(message = "Merchant name is required")
    @Size(min = 2, max = 255, message = "Merchant name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Merchant email is required")
    @Email(message = "Merchant email must be valid")
    @Size(max = 255, message = "Merchant email cannot exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    private String razorpayAccountId;

    private String webhookSecret;

    public MerchantRegisterRequestDto() {
    }

    public MerchantRegisterRequestDto(String name, String email, String password, String razorpayAccountId, String webhookSecret) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.razorpayAccountId = razorpayAccountId;
        this.webhookSecret = webhookSecret;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRazorpayAccountId() {
        return razorpayAccountId;
    }

    public void setRazorpayAccountId(String razorpayAccountId) {
        this.razorpayAccountId = razorpayAccountId;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
