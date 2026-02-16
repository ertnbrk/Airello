package ai.planmate.payments.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.UserPlan;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.payments.entity.SubscriptionEvent;
import ai.planmate.payments.entity.SubscriptionEventType;
import ai.planmate.payments.repository.SubscriptionEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "planmate.features.payments-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final SubscriptionEventRepository subscriptionEventRepository;
    private final AppUserRepository appUserRepository;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void handleWebhook(String payload, String signature) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature", e);
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        // Idempotency check
        if (subscriptionEventRepository.existsByStripeEventId(event.getId())) {
            log.info("Duplicate webhook event: { }", event.getId());
            return;
        }

        String eventType = event.getType();
        SubscriptionEventType ourEventType = mapEventType(eventType);

        if (ourEventType == null) {
            log.info("Ignoring event type: { }", eventType);
            return;
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if (stripeObject instanceof Subscription subscription) {
            processSubscription(event, subscription, ourEventType);
        }
    }

    private void processSubscription(
            Event event, Subscription subscription, SubscriptionEventType eventType) {
        String customerEmail =
                subscription.getCustomer() != null
                        ? subscription.getCustomerObject().getEmail()
                        : null;

        if (customerEmail == null) {
            log.error("No customer email in subscription event");
            return;
        }

        AppUser user =
                appUserRepository
                        .findByEmail(customerEmail)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User not found: " + customerEmail));

        SubscriptionEvent subscriptionEvent = new SubscriptionEvent();
        subscriptionEvent.setUser(user);
        subscriptionEvent.setEventType(eventType);
        subscriptionEvent.setStripeEventId(event.getId());
        subscriptionEvent.setStripeSubscriptionId(subscription.getId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("subscriptionId", subscription.getId());
        eventData.put("status", subscription.getStatus());
        subscriptionEvent.setEventData(eventData);

        subscriptionEventRepository.save(subscriptionEvent);

        // Update user plan based on subscription
        updateUserPlan(user, subscription);

        log.info("Processed subscription event: { }", event.getId());
    }

    private void updateUserPlan(AppUser user, Subscription subscription) {
        String status = subscription.getStatus();

        if ("active".equals(status)) {
            user.setPlan(UserPlan.PRO); // Simplified: all active = PRO
        } else if ("canceled".equals(status) || "incomplete_expired".equals(status)) {
            user.setPlan(UserPlan.FREE);
        }

        appUserRepository.save(user);
    }

    private SubscriptionEventType mapEventType(String stripeEventType) {
        return switch (stripeEventType) {
            case "customer.subscription.created" -> SubscriptionEventType.SUBSCRIPTION_CREATED;
            case "customer.subscription.updated" -> SubscriptionEventType.SUBSCRIPTION_UPDATED;
            case "customer.subscription.deleted" -> SubscriptionEventType.SUBSCRIPTION_CANCELED;
            case "invoice.payment_succeeded" -> SubscriptionEventType.PAYMENT_SUCCEEDED;
            case "invoice.payment_failed" -> SubscriptionEventType.PAYMENT_FAILED;
            default -> null;
        };
    }
}
