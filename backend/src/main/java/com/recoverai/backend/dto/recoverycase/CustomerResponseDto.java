package com.recoverai.backend.dto.recoverycase;

import com.recoverai.backend.entity.Customer;

import java.time.Instant;
import java.util.UUID;

public class CustomerResponseDto {

    private UUID id;
    private String razorpayCustomerId;
    private String name;
    private String email;
    private String phone;
    private Instant createdAt;

    public CustomerResponseDto() {
    }

    public CustomerResponseDto(UUID id, String razorpayCustomerId, String name, String email, String phone, Instant createdAt) {
        this.id = id;
        this.razorpayCustomerId = razorpayCustomerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    public static CustomerResponseDto fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponseDto(
                customer.getId(),
                customer.getRazorpayCustomerId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRazorpayCustomerId() {
        return razorpayCustomerId;
    }

    public void setRazorpayCustomerId(String razorpayCustomerId) {
        this.razorpayCustomerId = razorpayCustomerId;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
