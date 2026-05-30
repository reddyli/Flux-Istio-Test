package li.reddy.order.client;

import li.reddy.order.exception.StockServiceUnavailableException;
import li.reddy.order.exception.StockUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class StockClient {

    private final RestClient restClient;

    public StockClient(RestClient stockServiceRestClient) {
        this.restClient = stockServiceRestClient;
    }

    public void reserve(String sku, int quantity) {
        try {
            restClient.post()
                    .uri("/api/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReserveBody(sku, quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new StockUnavailableException(
                    "stock-service rejected reserve (" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            throw new StockServiceUnavailableException("stock-service error: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            throw new StockServiceUnavailableException("stock-service unreachable: " + e.getMessage());
        }
    }

    private record ReserveBody(String sku, int quantity) {}
}
