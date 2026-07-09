package com.company.bank_system.cfg;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.UserCache;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Cache serialization.
 *
 * <p>Each cache holds a known, fixed value type, so we use <b>typed</b> Jackson serializers instead
 * of a polymorphic one. The previous {@code RedisSerializer.json()} (GenericJackson…) writes an
 * empty list as a bare {@code []} but, on read, expects a type-id wrapper
 * ({@code ["java.util.ArrayList", []]}) — which blows up with
 * "expected VALUE_STRING … type id (for subtype of java.lang.Object)" for any user with no
 * accounts or cards. Typed serializers round-trip empty and non-empty collections cleanly.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JavaType accountListType = mapper.getTypeFactory()
                .constructCollectionType(List.class, AccountResponse.class);
        JavaType cardListType = mapper.getTypeFactory()
                .constructCollectionType(List.class, CardResponse.class);

        SerializationPair<?> userPair = SerializationPair.fromSerializer(
                new Jackson2JsonRedisSerializer<>(mapper, mapper.getTypeFactory().constructType(UserCache.class)));
        SerializationPair<?> accountsPair = SerializationPair.fromSerializer(
                new Jackson2JsonRedisSerializer<>(mapper, accountListType));
        SerializationPair<?> cardsPair = SerializationPair.fromSerializer(
                new Jackson2JsonRedisSerializer<>(mapper, cardListType));

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.serializeValuesWith(
                        SerializationPair.fromSerializer(RedisSerializer.json())))
                .withCacheConfiguration("currentUser",
                        base.entryTtl(Duration.ofMinutes(5)).serializeValuesWith(userPair))
                .withCacheConfiguration("accounts",
                        base.entryTtl(Duration.ofSeconds(60)).serializeValuesWith(accountsPair))
                .withCacheConfiguration("cards",
                        base.entryTtl(Duration.ofMinutes(2)).serializeValuesWith(cardsPair))
                .build();
    }
}
