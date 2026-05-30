package li.reddy.order.web;

import li.reddy.order.entity.Order;
import li.reddy.order.exception.IdempotencyConflictException;
import li.reddy.order.service.IdempotencyService;
import li.reddy.order.service.IdempotencyService.ClaimResult;
import li.reddy.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class OrderController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> create(
            @RequestBody CreateOrderRequest req,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(placeOrder(req));
        }

        ClaimResult claim = idempotencyService.claim(idempotencyKey);
        return switch (claim) {
            case ClaimResult.Fresh f -> processAndStore(req, idempotencyKey);
            case ClaimResult.InFlight i -> throw new IdempotencyConflictException(
                    "A request with this Idempotency-Key is already being processed");
            case ClaimResult.Replay r -> replay(r.json());
        };
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getOne(@PathVariable UUID id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping("/orders")
    public List<OrderResponse> getAll() {
        return orderService.getAllOrders().stream().map(OrderResponse::from).toList();
    }

    private OrderResponse placeOrder(CreateOrderRequest req) {
        Order order = orderService.placeOrder(req.sku(), req.quantity());
        return OrderResponse.from(order);
    }

    private ResponseEntity<OrderResponse> processAndStore(CreateOrderRequest req, String idempotencyKey) {
        OrderResponse response;
        try {
            response = placeOrder(req);
        } catch (RuntimeException e) {
            idempotencyService.release(idempotencyKey);
            throw e;
        }
        idempotencyService.complete(idempotencyKey, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private ResponseEntity<OrderResponse> replay(String storedJson) {
        OrderResponse cached = idempotencyService.deserialise(storedJson, OrderResponse.class)
                .orElseThrow(() -> new IllegalStateException("Stored idempotent response is unreadable"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(REPLAYED_HEADER, "true")
                .body(cached);
    }
}
