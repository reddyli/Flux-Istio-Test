package li.reddy.stock.exception;


import li.reddy.stock.web.StockResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnknownSkuException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> unknownSkuException(UnknownSkuException e) {
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> insufficientStockException(InsufficientStockException e) {
        return Map.of("message", e.getMessage());
    }


}
