package com.recoverai.backend.service.provider.email;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.util.CredentialMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component("smtpEmailProvider")
public class SmtpEmailProvider implements EmailProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);
    public static final String PROVIDER_NAME = "SMTP_EMAIL";

    private final RecoveryCommunicationProperties properties;
    private final SmtpTransport transport;

    @org.springframework.beans.factory.annotation.Autowired
    public SmtpEmailProvider(RecoveryCommunicationProperties properties) {
        this(properties, new DefaultSmtpTransport());
    }

    public SmtpEmailProvider(RecoveryCommunicationProperties properties, SmtpTransport transport) {
        this.properties = properties;
        this.transport = transport != null ? transport : new DefaultSmtpTransport();
    }

    @Override
    public CommunicationDeliveryResult sendEmail(EmailMessageRequest request) {
        if (request == null) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "INVALID_REQUEST",
                    "Request cannot be null", null, ProviderFailureType.VALIDATION);
        }

        String recipientEmail = request.getRecipientEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "MISSING_RECIPIENT_EMAIL",
                    "Recipient email is required", null, ProviderFailureType.VALIDATION);
        }

        RecoveryCommunicationProperties.SmtpProperties smtp = properties.getEmail().getSmtp();
        if (smtp == null || smtp.getHost() == null || smtp.getHost().isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "PROVIDER_MISCONFIGURED",
                    "SMTP host is not configured", null, ProviderFailureType.AUTHENTICATION);
        }

        String fromAddress = properties.getEmail().getFromAddress();
        if (fromAddress == null || fromAddress.isBlank()) {
            fromAddress = "recover@recoverai.io";
        }

        String subject = "Payment Recovery: Action Required for " + (request.getMerchantName() != null ? request.getMerchantName() : "Your Order");
        String body = buildEmailBody(request);

        log.info("[SMTP_EMAIL] Sending email via host={}:{} to recipient={}",
                smtp.getHost(), smtp.getPort(), CredentialMasker.maskEmail(recipientEmail));

        try {
            String messageId = transport.send(
                    smtp.getHost(),
                    smtp.getPort(),
                    smtp.isTlsEnabled(),
                    smtp.getUsername(),
                    smtp.getPassword(),
                    fromAddress,
                    recipientEmail,
                    subject,
                    body
            );

            if (messageId == null || messageId.isBlank()) {
                messageId = "smtp_" + UUID.randomUUID().toString().substring(0, 8);
            }

            String metadata = String.format("{\"provider\":\"%s\",\"deliveryId\":\"%s\",\"host\":\"%s\"}",
                    PROVIDER_NAME, messageId, smtp.getHost());

            return CommunicationDeliveryResult.success(
                    messageId,
                    PROVIDER_NAME,
                    "EMAIL_DISPATCHED",
                    "Email sent successfully via SMTP server",
                    metadata
            );

        } catch (SmtpException ex) {
            ProviderFailureType failureType = ex.getFailureType();
            log.warn("[SMTP_EMAIL] SMTP error: code={}, failureType={}, message={}",
                    ex.getSmtpCode(), failureType, ex.getMessage());

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "SMTP_ERROR_" + ex.getSmtpCode(),
                    ex.getMessage(),
                    String.format("{\"provider\":\"%s\",\"smtpCode\":%d}", PROVIDER_NAME, ex.getSmtpCode()),
                    failureType
            );

        } catch (Exception ex) {
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            log.error("[SMTP_EMAIL] Transport failure: {}", ex.getMessage());

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    failureType == ProviderFailureType.TIMEOUT ? "SMTP_TIMEOUT" : "SMTP_DISPATCH_ERROR",
                    ex.getMessage(),
                    null,
                    failureType
            );
        }
    }

    private String buildEmailBody(EmailMessageRequest request) {
        return String.format("Subject: Action Required\r\n\r\nHello %s,\n\nYour payment of %s %s to %s failed.\nPlease use this link to complete your payment:\n%s\n\nRecoverAI",
                request.getCustomerName() != null ? request.getCustomerName() : "Customer",
                request.getAmount() != null ? request.getAmount() : "",
                request.getCurrency() != null ? request.getCurrency() : "INR",
                request.getMerchantName() != null ? request.getMerchantName() : "Merchant",
                request.getRecoveryLink() != null ? request.getRecoveryLink() : "");
    }

    @Override
    public ProviderHealthResult checkHealth() {
        RecoveryCommunicationProperties.SmtpProperties smtp = properties.getEmail().getSmtp();
        if (smtp == null || smtp.getHost() == null || smtp.getHost().isBlank()) {
            return ProviderHealthResult.misconfigured(PROVIDER_NAME, "EMAIL", "SMTP host is not configured");
        }
        return ProviderHealthResult.available(PROVIDER_NAME, "EMAIL", "SMTP email adapter configured");
    }

    @Override
    public String getProviderIdentifier() {
        return "smtp";
    }

    @Override
    public String getProviderCategory() {
        return "EMAIL";
    }

    public interface SmtpTransport {
        String send(String host, int port, boolean tls, String username, String password,
                    String from, String to, String subject, String body) throws Exception;
    }

    public static class DefaultSmtpTransport implements SmtpTransport {
        @Override
        public String send(String host, int port, boolean tls, String username, String password,
                           String from, String to, String subject, String body) throws Exception {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setSoTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                readResponse(reader, 220);

                writer.println("EHLO recoverai.io");
                readResponse(reader, 250);

                if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                    writer.println("AUTH LOGIN");
                    readResponse(reader, 334);
                    writer.println(Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
                    readResponse(reader, 334);
                    writer.println(Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
                    readResponse(reader, 235);
                }

                writer.println("MAIL FROM:<" + from + ">");
                readResponse(reader, 250);

                writer.println("RCPT TO:<" + to + ">");
                readResponse(reader, 250);

                writer.println("DATA");
                readResponse(reader, 354);

                writer.println("From: " + from);
                writer.println("To: " + to);
                writer.println("Subject: " + subject);
                writer.println();
                writer.println(body);
                writer.println(".");
                readResponse(reader, 250);

                writer.println("QUIT");

                return "smtp_" + UUID.randomUUID().toString().substring(0, 10);
            }
        }

        private void readResponse(BufferedReader reader, int expectedCode) throws Exception {
            String line = reader.readLine();
            if (line == null) {
                throw new SmtpException(500, "Unexpected end of stream from SMTP server", ProviderFailureType.TRANSIENT);
            }
            int code = 0;
            if (line.length() >= 3) {
                try {
                    code = Integer.parseInt(line.substring(0, 3));
                } catch (NumberFormatException ignored) {
                }
            }

            // Consume multiline replies if any (e.g. 250-...)
            while (line.length() >= 4 && line.charAt(3) == '-') {
                line = reader.readLine();
                if (line == null) break;
            }

            if (code != expectedCode) {
                ProviderFailureType failureType = ProviderFailureType.UNKNOWN;
                if (code == 535) {
                    failureType = ProviderFailureType.AUTHENTICATION;
                } else if (code == 550 || code == 551 || code == 553 || code == 501) {
                    failureType = ProviderFailureType.VALIDATION;
                } else if (code == 421 || code == 450 || code == 451 || code == 452) {
                    failureType = ProviderFailureType.TRANSIENT;
                }
                throw new SmtpException(code, "SMTP server returned error: " + line, failureType);
            }
        }
    }

    public static class SmtpException extends RuntimeException {
        private final int smtpCode;
        private final ProviderFailureType failureType;

        public SmtpException(int smtpCode, String message, ProviderFailureType failureType) {
            super(message);
            this.smtpCode = smtpCode;
            this.failureType = failureType != null ? failureType : ProviderFailureType.UNKNOWN;
        }

        public int getSmtpCode() {
            return smtpCode;
        }

        public ProviderFailureType getFailureType() {
            return failureType;
        }
    }
}
