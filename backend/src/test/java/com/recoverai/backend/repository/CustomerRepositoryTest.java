package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Test Merchant")
                .email("merchant_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Should persist and retrieve customer with merchant relationship")
    void testCreateAndFindCustomer() {
        Customer customer = Customer.builder()
                .merchant(merchant)
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+919876543210")
                .razorpayCustomerId("cust_123456")
                .build();

        Customer saved = customerRepository.saveAndFlush(customer);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<Customer> found = customerRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
        assertEquals("john.doe@example.com", found.get().getEmail());
        assertEquals(merchant.getId(), found.get().getMerchant().getId());
    }

    @Test
    @DisplayName("Should find customers scoped to merchant")
    void testFindByMerchantId() {
        Customer c1 = Customer.builder().merchant(merchant).name("C1").email("c1@test.com").build();
        Customer c2 = Customer.builder().merchant(merchant).name("C2").email("c2@test.com").build();
        customerRepository.saveAndFlush(c1);
        customerRepository.saveAndFlush(c2);

        List<Customer> list = customerRepository.findByMerchantId(merchant.getId());
        assertEquals(2, list.size());

        Optional<Customer> byEmail = customerRepository.findByMerchantIdAndEmail(merchant.getId(), "c1@test.com");
        assertTrue(byEmail.isPresent());
        assertEquals("C1", byEmail.get().getName());
    }

    @Test
    @DisplayName("Should enforce unique constraint on merchant_id and email")
    void testUniqueMerchantCustomerEmail() {
        Customer c1 = Customer.builder().merchant(merchant).name("C1").email("same@test.com").build();
        customerRepository.saveAndFlush(c1);

        Customer c2 = Customer.builder().merchant(merchant).name("C2").email("same@test.com").build();
        assertThrows(DataIntegrityViolationException.class, () -> {
            customerRepository.saveAndFlush(c2);
        });
    }

    @Test
    @DisplayName("Should allow same email across different merchants")
    void testSameEmailDifferentMerchants() {
        Merchant merchant2 = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Other Merchant")
                .email("other_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer c1 = Customer.builder().merchant(merchant).name("C1").email("shared@customer.com").build();
        Customer c2 = Customer.builder().merchant(merchant2).name("C2").email("shared@customer.com").build();

        customerRepository.saveAndFlush(c1);
        Customer saved2 = customerRepository.saveAndFlush(c2);

        assertNotNull(saved2.getId());
        assertEquals(merchant2.getId(), saved2.getMerchant().getId());
    }
}
