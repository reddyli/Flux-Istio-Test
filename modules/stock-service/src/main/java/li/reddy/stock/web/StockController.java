package li.reddy.stock.web;

import li.reddy.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StockController {

    StockService stockService;

    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/reserve")
    public ReserveResponse reserve(@RequestBody ReserveRequest request) {
        stockService.reserve(request.sku(),  request.quantity());
        return new ReserveResponse(true);
    }

    @GetMapping("/stock/{sku}")
    public StockResponse getStock(@PathVariable String sku) {
        return stockService.getStock(sku);
    }

    @PutMapping("/stock/{sku}/replenish")
    public StockResponse replenish(@PathVariable String sku, @RequestBody ReplenishRequest req) {
        stockService.replenish(sku, req.quantity());
        return stockService.getStock(sku);
    }

}
