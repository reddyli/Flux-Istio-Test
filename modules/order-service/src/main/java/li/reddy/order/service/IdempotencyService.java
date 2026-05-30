package li.reddy.order.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private static final String KEY_PREFIX = "idem:order:";
    private static final String PENDING_MARKER = "{\"_state\":\"pending\"}";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public ClaimResult claim(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        Boolean claimed = redis.opsForValue().setIfAbsent(key, PENDING_MARKER, TTL);
        if (Boolean.TRUE.equals(claimed)) {
            return ClaimResult.fresh();
        }
        String existing = redis.opsForValue().get(key);
        if (existing == null) {
            // race: TTL expired between SETNX and GET; treat as fresh on retry
            return ClaimResult.fresh();
        }
        if (PENDING_MARKER.equals(existing)) {
            return ClaimResult.inFlight();
        }
        return ClaimResult.replay(existing);
    }

    public <T> void complete(String idempotencyKey, T response) {
        String key = KEY_PREFIX + idempotencyKey;
        try {
            String json = objectMapper.writeValueAsString(response);
            redis.opsForValue().set(key, json, TTL);
        } catch (JacksonException e) {
            log.warn("Failed to serialise idempotent response for key {}; releasing claim", idempotencyKey, e);
            release(idempotencyKey);
        }
    }

    public void release(String idempotencyKey) {
        redis.delete(KEY_PREFIX + idempotencyKey);
    }

    public <T> Optional<T> deserialise(String json, Class<T> type) {
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JacksonException e) {
            log.warn("Failed to deserialise stored idempotent response", e);
            return Optional.empty();
        }
    }

    public sealed interface ClaimResult {
        static ClaimResult fresh() { return new Fresh(); }
        static ClaimResult inFlight() { return new InFlight(); }
        static ClaimResult replay(String json) { return new Replay(json); }

        record Fresh() implements ClaimResult {}
        record InFlight() implements ClaimResult {}
        record Replay(String json) implements ClaimResult {}
    }
}
