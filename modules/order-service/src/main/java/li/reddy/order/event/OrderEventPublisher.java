package li.reddy.order.event;

import li.reddy.common.event.OrderCreatedEvent;
import org.apache.pulsar.client.api.Schema;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final String TOPIC = "persistent://public/default/orders.created";

    private final PulsarTemplate<OrderCreatedEvent> pulsarTemplate;

    public OrderEventPublisher(PulsarTemplate<OrderCreatedEvent> pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    public void publish(OrderCreatedEvent event) {
        pulsarTemplate.send(TOPIC, event, Schema.JSON(OrderCreatedEvent.class));
    }
}
