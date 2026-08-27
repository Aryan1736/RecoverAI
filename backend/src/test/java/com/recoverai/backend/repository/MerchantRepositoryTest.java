package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class MerchantRepositoryTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should persist and retrieve merchant with generated timestamps and UUID")
    void testCreateAndFindMerchant() {
        Merchant merchant = Merchant.builder()
                .name("Acme Corp")
                .email("billing@acmecorp.com")
                .razorpayAccountId("acc_123456")
                .webhookSecret("whsec_secret_key")
                .status(MerchantStatus.ACTIVE)
                .build();

        Merchant saved = merchantRepository.saveAndFlush(merchant);

        assertNotNull(saved.getId(), "Merchant ID should be auto-generated");
        assertNotNull(saved.getCreatedAt(), "createdAt timestamp should be auto-set");
        assertNotNull(saved.getUpdatedAt(), "updatedAt timestamp should be auto-set");

        Optional<Merchant> found = merchantRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Acme Corp", found.get().getName());
        assertEquals("billing@acmecorp.com", found.get().getEmail());
        assertEquals(MerchantStatus.ACTIVE, found.get().getStatus());
    }

    @Test
    @DisplayName("Should find merchant by email and razorpayAccountId")
    void testFindByEmailAndRazorpayAccountId() {
        Merchant merchant = Merchant.builder()
                .name("Nova Retail")
                .email("contact@novaretail.io")
                .razorpayAccountId("acc_nova_001")
                .status(MerchantStatus.ACTIVE)
                .build();
        merchantRepository.saveAndFlush(merchant);

        Optional<Merchant> byEmail = merchantRepository.findByEmail("contact@novaretail.io");
        assertTrue(byEmail.isPresent());
        assertEquals("Nova Retail", byEmail.get().getName());

        Optional<Merchant> byRazorpay = merchantRepository.findByRazorpayAccountId("acc_nova_001");
        assertTrue(byRazorpay.isPresent());
        assertEquals("contact@novaretail.io", byRazorpay.get().getEmail());

        assertTrue(merchantRepository.existsByEmail("contact@novaretail.io"));
        assertFalse(merchantRepository.existsByEmail("nonexistent@domain.com"));
    }

    @Test
    @DisplayName("Should filter merchants by status")
    void testFindByStatus() {
        Merchant m1 = Merchant.builder().name("M1").email("m1@test.com").status(MerchantStatus.ACTIVE).build();
        Merchant m2 = Merchant.builder().name("M2").email("m2@test.com").status(MerchantStatus.INACTIVE).build();
        merchantRepository.saveAndFlush(m1);
        merchantRepository.saveAndFlush(m2);

        List<Merchant> active = merchantRepository.findByStatus(MerchantStatus.ACTIVE);
        assertTrue(active.stream().anyMatch(m -> m.getEmail().equals("m1@test.com")));
        assertFalse(active.stream().anyMatch(m -> m.getEmail().equals("m2@test.com")));
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void testUniqueEmailConstraint() {
        Merchant m1 = Merchant.builder().name("M1").email("dup@test.com").build();
        merchantRepository.saveAndFlush(m1);

        Merchant m2 = Merchant.builder().name("M2").email("dup@test.com").build();
        assertThrows(DataIntegrityViolationException.class, () -> {
            merchantRepository.saveAndFlush(m2);
        });
    }
}
