package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByMerchantId(UUID merchantId);

    Page<Customer> findByMerchantId(UUID merchantId, Pageable pageable);

    Optional<Customer> findByMerchantIdAndEmail(UUID merchantId, String email);

    Optional<Customer> findByMerchantIdAndRazorpayCustomerId(UUID merchantId, String razorpayCustomerId);

    Optional<Customer> findByIdAndMerchantId(UUID id, UUID merchantId);

    boolean existsByMerchantIdAndEmail(UUID merchantId, String email);
}
