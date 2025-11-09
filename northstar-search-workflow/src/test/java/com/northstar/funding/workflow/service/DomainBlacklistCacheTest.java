package com.northstar.funding.workflow.service;

import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.persistence.service.DomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for DomainBlacklistCache (Valkey integration).
 *
 * <p>Tests read-through caching, TTL, fallback to PostgreSQL, and cache invalidation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainBlacklistCacheTest {

    @Mock
    private RedisTemplate<String, Boolean> redisTemplate;

    @Mock
    private ValueOperations<String, Boolean> valueOps;

    @Mock
    private DomainService domainService;

    private DomainBlacklistCache cache;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cache = new DomainBlacklistCache(redisTemplate, domainService);
    }

    @Test
    void isBlacklisted_whenCacheHit_shouldReturnCachedValue() {
        // Given - domain is in cache and blacklisted
        when(valueOps.get("blacklist:spam.xyz")).thenReturn(true);

        // When
        boolean result = cache.isBlacklisted("spam.xyz");

        // Then
        assertThat(result).isTrue();
        verify(valueOps).get("blacklist:spam.xyz");
        verifyNoInteractions(domainService); // Should NOT hit database
    }

    @Test
    void isBlacklisted_whenCacheMiss_shouldQueryDatabaseAndCache() {
        // Given - cache miss, database says blacklisted
        when(valueOps.get("blacklist:malware.info")).thenReturn(null);

        Domain domain = Domain.builder()
                .domainId(UUID.randomUUID())
                .domainName("malware.info")
                .status(DomainStatus.BLACKLISTED)
                .discoveredAt(LocalDateTime.now())
                .build();

        when(domainService.findByDomainName("malware.info")).thenReturn(Optional.of(domain));

        // When
        boolean result = cache.isBlacklisted("malware.info");

        // Then
        assertThat(result).isTrue();
        verify(valueOps).get("blacklist:malware.info");
        verify(domainService).findByDomainName("malware.info");
        verify(valueOps).set("blacklist:malware.info", true, 24, TimeUnit.HOURS); // 24-hour TTL
    }

    @Test
    void isBlacklisted_whenDomainNotFoundInDatabase_shouldCacheFalse() {
        // Given - cache miss, domain not in database (new domain)
        when(valueOps.get("blacklist:newdomain.edu")).thenReturn(null);
        when(domainService.findByDomainName("newdomain.edu")).thenReturn(Optional.empty());

        // When
        boolean result = cache.isBlacklisted("newdomain.edu");

        // Then
        assertThat(result).isFalse();
        verify(valueOps).set("blacklist:newdomain.edu", false, 24, TimeUnit.HOURS);
    }

    @Test
    void isBlacklisted_whenDomainNotBlacklisted_shouldCacheFalse() {
        // Given - domain exists but not blacklisted
        when(valueOps.get("blacklist:legit.edu")).thenReturn(null);

        Domain domain = Domain.builder()
                .domainId(UUID.randomUUID())
                .domainName("legit.edu")
                .status(DomainStatus.PROCESSED_HIGH_QUALITY)
                .discoveredAt(LocalDateTime.now())
                .build();

        when(domainService.findByDomainName("legit.edu")).thenReturn(Optional.of(domain));

        // When
        boolean result = cache.isBlacklisted("legit.edu");

        // Then
        assertThat(result).isFalse();
        verify(valueOps).set("blacklist:legit.edu", false, 24, TimeUnit.HOURS);
    }

    @Test
    void isBlacklisted_whenValkeyUnavailable_shouldFallbackToDatabase() {
        // Given - Valkey throws exception (connection refused)
        when(valueOps.get("blacklist:example.org"))
                .thenThrow(new RuntimeException("Connection refused"));

        Domain domain = Domain.builder()
                .domainId(UUID.randomUUID())
                .domainName("example.org")
                .status(DomainStatus.DISCOVERED)
                .discoveredAt(LocalDateTime.now())
                .build();

        when(domainService.findByDomainName("example.org")).thenReturn(Optional.of(domain));

        // When
        boolean result = cache.isBlacklisted("example.org");

        // Then
        assertThat(result).isFalse();
        verify(domainService).findByDomainName("example.org");
        // Should NOT attempt to cache when Valkey is down
    }

    @Test
    void invalidate_shouldRemoveFromCache() {
        // When
        cache.invalidate("updated.com");

        // Then
        verify(redisTemplate).delete("blacklist:updated.com");
    }

    @Test
    void invalidate_whenValkeyUnavailable_shouldHandleGracefully() {
        // Given
        when(redisTemplate.delete("blacklist:example.com"))
                .thenThrow(new RuntimeException("Connection refused"));

        // When/Then - should not throw exception
        assertThatCode(() -> cache.invalidate("example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void isBlacklisted_withNullDomain_shouldReturnFalse() {
        // When/Then
        assertThat(cache.isBlacklisted(null)).isFalse();
        verifyNoInteractions(valueOps, domainService);
    }

    @Test
    void isBlacklisted_withEmptyDomain_shouldReturnFalse() {
        // When/Then
        assertThat(cache.isBlacklisted("")).isFalse();
        assertThat(cache.isBlacklisted("   ")).isFalse();
        verifyNoInteractions(valueOps, domainService);
    }
}
