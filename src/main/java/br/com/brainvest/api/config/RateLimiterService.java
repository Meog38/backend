package br.com.brainvest.api.config;

import br.com.brainvest.api.api.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final Clock clock;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public RateLimiterService() {
        this(Clock.systemUTC());
    }

    RateLimiterService(Clock clock) {
        this.clock = clock;
    }

    public void check(String bucket, String key, int maxAttempts, Duration window) {
        String normalizedKey = bucket + ":" + (key == null || key.isBlank() ? "unknown" : key.trim().toLowerCase());
        Instant now = Instant.now(clock);
        Instant oldestAllowed = now.minus(window);
        Deque<Instant> bucketAttempts = attempts.computeIfAbsent(normalizedKey, ignored -> new ArrayDeque<>());

        synchronized (bucketAttempts) {
            while (!bucketAttempts.isEmpty() && bucketAttempts.peekFirst().isBefore(oldestAllowed)) {
                bucketAttempts.removeFirst();
            }
            if (bucketAttempts.size() >= maxAttempts) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                        "Muitas tentativas em pouco tempo. Aguarde alguns minutos e tente novamente.");
            }
            bucketAttempts.addLast(now);
        }
    }
}
