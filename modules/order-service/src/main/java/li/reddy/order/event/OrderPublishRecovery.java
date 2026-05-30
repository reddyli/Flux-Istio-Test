package li.reddy.order.event;

import li.reddy.common.event.OrderCreatedEvent;
import li.reddy.order.entity.Order;
import li.reddy.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class OrderPublishRecovery {

    private static final Logger log = LoggerFactory.getLogger(OrderPublishRecovery.class);
    private static final int BATCH_SIZE = 500;

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderPublishRecovery(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        log.info("scanning for unpublished CONFIRMED orders...");
        int republished = 0;
        int failed = 0;
        int batches = 0;

        while (true) {
            List<Order> batch = orderRepository.findUnpublishedConfirmed(PageRequest.of(0, BATCH_SIZE));
            if (batch.isEmpty()) break;

            int progress = 0;
            for (Order order : batch) {
                if (republishOne(order)) {
                    republished++;
                    progress++;
                } else {
                    failed++;
                }
            }
            batches++;
            if (progress == 0) {
                log.warn("startup recovery: no progress in batch of {}, aborting loop", batch.size());
                break;
            }
            if (batch.size() < BATCH_SIZE) break;
        }

        if (republished == 0 && failed == 0) {
            log.info("startup recovery: nothing to do");
        } else {
            log.info("startup recovery: republished={} failed={} batches={}", republished, failed, batches);
        }
    }

    private boolean republishOne(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getEventId(),
                order.getId(),
                order.getSku(),
                order.getQuantity(),
                order.getCreatedAt());
        try {
            orderEventPublisher.publish(event);
            order.setPublishedAt(Instant.now());
            orderRepository.save(order);
            log.info("recovered order {} eventId={}", order.getId(), order.getEventId());
            return true;
        } catch (Exception e) {
            log.warn("recovery publish failed for order {}; will retry on next startup", order.getId(), e);
            return false;
        }
    }
}
