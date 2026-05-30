package li.reddy.stock.event;

import li.reddy.common.event.OrderCreatedEvent;
import li.reddy.stock.entity.OrderAudit;
import li.reddy.stock.repository.OrderAuditRepository;
import org.apache.pulsar.common.schema.SchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderAuditRepository orderAuditRepository;

    public OrderEventListener(OrderAuditRepository orderAuditRepository) {
        this.orderAuditRepository = orderAuditRepository;
    }

    @PulsarListener(
            topics = "persistent://public/default/orders.created",
            subscriptionName = "stock-service-audit",
            schemaType = SchemaType.JSON
    )
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("received OrderCreatedEvent eventId={} orderId={}", event.eventId(), event.orderId());

        if (orderAuditRepository.existsById(event.eventId())) {
            log.info("duplicate event {}, ignoring", event.eventId());
            return;
        }

        try {
            OrderAudit audit = new OrderAudit(
                    event.eventId(),
                    event.orderId(),
                    event.sku(),
                    event.quantity(),
                    Instant.now()
            );
            orderAuditRepository.save(audit);
        } catch (DataIntegrityViolationException e) {
            log.info("duplicate event {} (race-detected), ignoring", event.eventId());
        }
    }
}
