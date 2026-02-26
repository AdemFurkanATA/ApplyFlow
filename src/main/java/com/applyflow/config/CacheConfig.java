package com.applyflow.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

        private static final String CACHE_APPLICATIONS = "applications";
        private static final String CACHE_USER_APPLICATIONS = "userApplications";

        @Value("${application.cache.ttl-seconds:60}")
        private long ttlSeconds;

        @Value("${application.cache.enabled:true}")
        private boolean cacheEnabled;

        @Bean
        @Profile("prod")
        public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
                if (!cacheEnabled) {
                        log.info("Cache is disabled, using no-op cache manager");
                        return new ConcurrentMapCacheManager();
                }

                ObjectMapper cacheMapper = new ObjectMapper();
                cacheMapper.registerModule(new JavaTimeModule());
                cacheMapper.activateDefaultTyping(
                                LaissezFaireSubTypeValidator.instance,
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheMapper);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofSeconds(ttlSeconds))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                                .disableCachingNullValues();

                log.info("Redis cache configured with TTL={}s, enabled={}", ttlSeconds, cacheEnabled);

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withCacheConfiguration(CACHE_APPLICATIONS,
                                                defaultConfig.entryTtl(Duration.ofSeconds(ttlSeconds)))
                                .withCacheConfiguration(CACHE_USER_APPLICATIONS,
                                                defaultConfig.entryTtl(Duration.ofSeconds(ttlSeconds)))
                                .build();
        }

        @Bean
        @Profile("!prod")
        public CacheManager simpleCacheManager() {
                if (!cacheEnabled) {
                        log.info("Cache is disabled in non-prod profile");
                        return new ConcurrentMapCacheManager();
                }
                return new ConcurrentMapCacheManager(CACHE_APPLICATIONS, CACHE_USER_APPLICATIONS);
        }
}
