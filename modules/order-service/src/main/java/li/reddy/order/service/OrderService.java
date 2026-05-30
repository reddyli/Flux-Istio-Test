package li.reddy.order.service;

import li.reddy.common.event.OrderCreatedEvent;
import li.reddy.order.client.StockClient;
import li.reddy.order.entity.Order;
import li.reddy.order.entity.OrderStatus;
import li.reddy.order.exception.OrderNotFoundException;
import li.reddy.order.exception.StockUnavailableException;
import li.reddy.order.repository.OrderRepository;
import li.reddy.order.event.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final StockClient stockClient;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, StockClient stockClient, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.stockClient = stockClient;
        this.orderEventPublisher = orderEventPublisher;
    }

    public Order placeOrder(String sku, int quantity) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        try {
            stockClient.reserve(sku, quantity);
        } catch (StockUnavailableException e) {
            Order rejected = new Order(id, sku, quantity, OrderStatus.REJECTED, e.getMessage(), now, null);
            return orderRepository.save(rejected);
        }

        UUID eventId = UUID.randomUUID();
        Order order = new Order(id, sku, quantity, OrderStatus.CONFIRMED, null, now, eventId);
        order = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId, order.getId(), order.getSku(), order.getQuantity(), order.getCreatedAt());

        try {
            orderEventPublisher.publish(event);
            order.setPublishedAt(Instant.now());
            order = orderRepository.save(order);
        } catch (Exception e) {
            log.warn("Order {} saved but publish failed; will retry on startup", order.getId(), e);
        }

        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
