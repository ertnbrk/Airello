package ai.planmate.payments.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.planmate.payments.entity.SubscriptionEvent;

@Repository
public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, UUID> {

    Optional<SubscriptionEvent> findByStripeEventId(String stripeEventId);

    boolean existsByStripeEventId(String stripeEventId);
}
