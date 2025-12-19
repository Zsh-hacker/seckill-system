package com.zsh.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager caffeineCacheManager(CacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(properties.getLocal().getInitialCapacity())
                .maximumSize(properties.getLocal().getMaximumSize())
                .expireAfterWrite(properties.getLocal().getExpireAfterWrite().toMillis(), TimeUnit.MILLISECONDS)
                .expireAfterAccess(properties.getLocal().getExpireAfterAccess().toMillis(), TimeUnit.MILLISECONDS)
                .recordStats());

        return cacheManager;
    }
}
