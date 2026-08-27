package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByMerchantId(UUID merchantId);

    Page<Payment> findByMerchantId(UUID merchantId, Pageable pageable);

    List<Payment> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status);

    Page<Payment> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status, Pageable pageable);

    Optional<Payment> findByMerchantIdAndRazorpayPaymentId(UUID merchantId, String razorpayPaymentId);

    Optional<Payment> findByIdAndMerchantId(UUID id, UUID merchantId);

    List<Payment> findByMerchantIdAndCustomerId(UUID merchantId, UUID customerId);

    List<Payment> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByMerchantIdAndRazorpayPaymentId(UUID merchantId, String razorpayPaymentId);
}
