package li.reddy.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(OrderNotFoundException e) {
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(StockServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> stockServiceDown(StockServiceUnavailableException e) {
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> idempotencyConflict(IdempotencyConflictException e) {
        return Map.of("message", e.getMessage());
    }
}
