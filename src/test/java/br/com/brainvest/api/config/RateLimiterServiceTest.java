package br.com.brainvest.api.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.brainvest.api.api.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

    @Test
    void blocksRequestsAfterBucketLimitIsReached() {
        RateLimiterService limiter = new RateLimiterService(
                Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC));

        assertDoesNotThrow(() -> limiter.check("login", "user@example.com", 2, Duration.ofMinutes(10)));
        assertDoesNotThrow(() -> limiter.check("login", "user@example.com", 2, Duration.ofMinutes(10)));

        assertThrows(ApiException.class,
                () -> limiter.check("login", "user@example.com", 2, Duration.ofMinutes(10)));
    }
}
