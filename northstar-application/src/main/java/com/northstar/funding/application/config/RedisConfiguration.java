package com.northstar.funding.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * Redis/Valkey configuration for blacklist caching.
 *
 * <p>Configures RedisTemplate for domain blacklist lookups with:
 * <ul>
 *   <li>String keys (domain names)</li>
 *   <li>Boolean values (blacklist status)</li>
 *   <li>JSON serialization for values</li>
 * </ul>
 */
@Configuration
public class RedisConfiguration {

    /**
     * Creates RedisTemplate for String keys and Boolean values.
     *
     * Used by DomainBlacklistCache for fast blacklist lookups.
     *
     * @param connectionFactory Auto-configured Redis connection factory
     * @return Configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Boolean> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Boolean> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
