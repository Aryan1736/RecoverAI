package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant merchant;
    private Customer customer;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Payment Merchant")
                .email("paymerchant_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .name("Payment Customer")
                .email("paycustomer_" + UUID.randomUUID() + "@test.com")
                .build());
    }

    @Test
    @DisplayName("Should persist and retrieve payment with failure details and relationships")
    void testCreateAndFindPayment() {
        Payment payment = Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_failed_123")
                .razorpayOrderId("order_abc_456")
                .razorpayInvoiceId("inv_789")
                .amount(new BigDecimal("4999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .errorCode("BAD_REQUEST_PAYMENT_DECLINED")
                .errorDescription("Card issuer declined transaction due to insufficient funds")
                .errorSource("issuer")
                .errorReason("insufficient_funds")
                .riskLevel(RiskLevel.MEDIUM)
                .paymentCreatedAt(Instant.now())
                .build();

        Payment saved = paymentRepository.saveAndFlush(payment);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<Payment> found = paymentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("pay_failed_123", found.get().getRazorpayPaymentId());
        assertEquals(PaymentStatus.FAILED, found.get().getStatus());
        assertEquals(new BigDecimal("4999.00"), found.get().getAmount());
        assertEquals(merchant.getId(), found.get().getMerchant().getId());
        assertEquals(customer.getId(), found.get().getCustomer().getId());
    }

    @Test
    @DisplayName("Should filter payments by merchant and status")
    void testFindByMerchantAndStatus() {
        Payment p1 = Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_001")
                .amount(new BigDecimal("1000.00"))
                .status(PaymentStatus.FAILED)
                .build();
        Payment p2 = Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_002")
                .amount(new BigDecimal("2000.00"))
                .status(PaymentStatus.CAPTURED)
                .build();
        paymentRepository.saveAndFlush(p1);
        paymentRepository.saveAndFlush(p2);

        List<Payment> failed = paymentRepository.findByMerchantIdAndStatus(merchant.getId(), PaymentStatus.FAILED);
        assertEquals(1, failed.size());
        assertEquals("pay_001", failed.get(0).getRazorpayPaymentId());
    }

    @Test
    @DisplayName("Should enforce unique constraint on merchant_id and razorpay_payment_id")
    void testUniqueRazorpayPaymentIdPerMerchant() {
        Payment p1 = Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_dup_001")
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.FAILED)
                .build();
        paymentRepository.saveAndFlush(p1);

        Payment p2 = Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_dup_001")
                .amount(new BigDecimal("750.00"))
                .status(PaymentStatus.FAILED)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            paymentRepository.saveAndFlush(p2);
        });
    }
}
