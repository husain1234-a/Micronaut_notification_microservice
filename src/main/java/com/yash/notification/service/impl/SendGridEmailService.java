package com.yash.notification.service.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.yash.notification.config.SendGridConfig;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import io.github.resilience4j.micronaut.annotation.CircuitBreaker;
import io.micronaut.retry.annotation.Recoverable;

@Singleton
public class SendGridEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(SendGridEmailService.class);

    private final SendGrid sendGridClient;
    private final String fromEmail;

    public SendGridEmailService(SendGridConfig sendGridConfig) {
        this.sendGridClient = new SendGrid(sendGridConfig.getApiKey());
        this.fromEmail = "en20cs301184@medicaps.ac.in"; // Use the from email from your application.yml
    }

    @CircuitBreaker(name = "sendGrid", fallbackMethod = "sendEmailFallback")
    public boolean sendEmail(String to, String subject, String bodyPlainText, String bodyHtmlText) {
        Email from = new Email(fromEmail);
        Email toEmail = new Email(to);
        Content contentPlainText = new Content("text/plain", bodyPlainText);
        Content contentHtmlText = new Content("text/html", bodyHtmlText);

        Mail mail = new Mail(from, subject, toEmail, contentHtmlText); // Prefer HTML

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);

            LOG.info("SendGrid email sent. Status code: {}", response.getStatusCode());
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                return true;
            } else {
                throw new RuntimeException("SendGrid returned status code: " + response.getStatusCode());
            }

        } catch (IOException e) {
            LOG.error("Failed to send email via SendGrid", e);
            throw new RuntimeException("Failed to send email via SendGrid", e);
        }
    }

    public boolean sendEmailFallback(String to, String subject, String bodyPlainText, String bodyHtmlText, Throwable t) {
        LOG.error("[CIRCUIT BREAKER] SendGrid fallback triggered for to: {} subject: {}. Reason: {}", to, subject,
                t.getMessage());
        return false;
    }
}