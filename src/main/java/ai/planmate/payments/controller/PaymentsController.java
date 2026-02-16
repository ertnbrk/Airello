package ai.planmate.payments.controller;

import java.util.Map;

import org.hibernate.validator.constraints.URL;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.planmate.payments.service.BillingService;
import ai.planmate.payments.service.StripeWebhookService;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(name = "planmate.features.payments-enabled", havingValue = "true")
public class PaymentsController {

    private final StripeWebhookService webhookService;
    private final BillingService billingService;

    @PostMapping("/stripe/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void handleStripeWebhook(
            @NotBlank(message = "Webhook payload is required") @RequestBody String payload,
            @NotBlank(message = "Stripe signature is required") @RequestHeader("Stripe-Signature")
                    String signature) {
        webhookService.handleWebhook(payload, signature);
    }

    @GetMapping("/billing-portal")
    public Map<String, String> getBillingPortal(
            @URL(message = "Return URL must be a valid URL")
                    @RequestParam(defaultValue = "http://localhost:3000")
                    String returnUrl) {
        String url = billingService.createBillingPortalSession(returnUrl);
        return Map.of("url", url);
    }
}
