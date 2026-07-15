package com.example.Projectly.config.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenDenylist implements TokenDenylist {

    // jti → expiry epoch second
    private final Map<String, Long> store = new ConcurrentHashMap<>();

    @Override
    public void add(final String jti, final long ttlSeconds) {
        this.store.put(jti, Instant.now().getEpochSecond() + ttlSeconds);
    }

    @Override
    public boolean isDenied(final String jti) {
        final Long expiry = this.store.get(jti);
        if (expiry == null) return false;
        if (Instant.now().getEpochSecond() > expiry) {
            this.store.remove(jti);
            return false;
        }
        return true;
    }

    // Purge expired entries every 5 minutes to prevent unbounded growth
    @Scheduled(fixedDelay = 300_000)
    public void evictExpired() {
        final long now = Instant.now().getEpochSecond();
        this.store.entrySet().removeIf(e -> e.getValue() < now);
    }
}
