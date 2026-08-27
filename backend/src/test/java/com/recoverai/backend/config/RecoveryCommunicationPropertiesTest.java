package com.recoverai.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryCommunicationPropertiesTest {

    @Test
    @DisplayName("Should initialize with sensible default configuration")
    void shouldHaveSensibleDefaults() {
        RecoveryCommunicationProperties props = new RecoveryCommunicationProperties();

        assertThat(props.getBaseUrl()).isEqualTo("https://pay.recoverai.io/r/");
        assertThat(props.getWhatsapp().getProvider()).isEqualTo("mock");
        assertThat(props.getWhatsapp().getSenderNumber()).isEqualTo("+14155238886");
        assertThat(props.getEmail().getProvider()).isEqualTo("mock");
        assertThat(props.getEmail().getFromAddress()).isEqualTo("recover@recoverai.io");
        assertThat(props.getSms().getProvider()).isEqualTo("mock");
        assertThat(props.getSms().getSenderId()).isEqualTo("RECOVER");
        assertThat(props.getRetryCharge().getProvider()).isEqualTo("mock");
        assertThat(props.getRetryCharge().isAutoRetryEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should permit custom property setters")
    void shouldPermitCustomSetters() {
        RecoveryCommunicationProperties props = new RecoveryCommunicationProperties();
        props.setBaseUrl("https://custom.pay.io/r/");
        props.getWhatsapp().setSenderNumber("+919999999999");
        props.getEmail().setFromAddress("support@custom.io");
        props.getSms().setSenderId("CUSTOM");
        props.getRetryCharge().setAutoRetryEnabled(false);

        assertThat(props.getBaseUrl()).isEqualTo("https://custom.pay.io/r/");
        assertThat(props.getWhatsapp().getSenderNumber()).isEqualTo("+919999999999");
        assertThat(props.getEmail().getFromAddress()).isEqualTo("support@custom.io");
        assertThat(props.getSms().getSenderId()).isEqualTo("CUSTOM");
        assertThat(props.getRetryCharge().isAutoRetryEnabled()).isFalse();
    }
}
