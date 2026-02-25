package ai.planmate.payments.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ai.planmate.auth.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnProperty(name = "planmate.features.payments-enabled", havingValue = "true")
@RequiredArgsConstructor
public class BillingService {

    private final AppUserRepository appUserRepository;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    public String createBillingPortalSession(String returnUrl) {
        // TODO: Without authentication, implement user identification mechanism (e.g., pass userId
        // as parameter)
        throw new UnsupportedOperationException(
                "Billing portal requires authentication to be enabled");
        /* Original code before removing authentication:
        Stripe.apiKey = stripeApiKey;

        UUID currentUserId = securityContext.getCurrentUserId();
        AppUser user =
                appUserRepository
                        .findById(currentUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStripeCustomerId() == null) {
            throw new IllegalStateException("User has no Stripe customer ID");
        }

        try {
            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setCustomer(user.getStripeCustomerId())
                            .setReturnUrl(returnUrl)
                            .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create billing portal session", e);
        }
        */
    }
}
