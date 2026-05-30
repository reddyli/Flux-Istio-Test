package li.reddy.stock.service;

import li.reddy.stock.web.StockResponse;
import tools.jackson.core.JacksonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
public class StockCacheService {

    /*
    *   Design for cache lookup for stock
    *   need 3 methods, GET, DEL, PUT keys
    *
    * */

    private static final String KEY_PREFIX = "stock:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;


    @Autowired
    public StockCacheService(StringRedisTemplate stringRedisTemplate,  ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<StockResponse> getStock(String sku) {
        String responseJson = stringRedisTemplate.opsForValue().get(KEY_PREFIX + sku);
        if (responseJson == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(responseJson, StockResponse.class));
        }  catch (JacksonException e) {
            return Optional.empty();
        }

    }

    public void putStock(String sku, StockResponse response) {
        try {
            String stockResponseJson = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + sku, stockResponseJson, TTL);
        } catch(JacksonException _) {}
    }

    public void invalidate(String sku) {
        stringRedisTemplate.delete(KEY_PREFIX + sku);
    }

}
