package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByEmail(String email);

    Optional<Merchant> findByRazorpayAccountId(String razorpayAccountId);

    List<Merchant> findByStatus(MerchantStatus status);

    boolean existsByEmail(String email);

    boolean existsByRazorpayAccountId(String razorpayAccountId);
}
