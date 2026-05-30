package li.reddy.order.exception;

public class StockServiceUnavailableException extends RuntimeException {
    public StockServiceUnavailableException(String message) {
        super(message);
    }
}
