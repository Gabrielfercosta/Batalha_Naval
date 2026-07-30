package com.batalha.Batalha_Naval.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A lista de salas abertas é consultada a cada 5 segundos por cada jogador no lobby.
 * O cache evita varrer o mapa de partidas em cada requisição.
 *
 * O TTL curto (5s) é essencial: é ele que garante que uma sala recém-criada apareça
 * para os outros jogadores mesmo quando o @CacheEvict não dispara — o que acontece
 * nos modos quiz e minado, onde criarPartida() é chamado internamente pelo próprio
 * serviço e portanto não passa pelo proxy do Spring.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Duration TTL_SALAS = Duration.ofSeconds(5);

    /**
     * Cache Redis — usado quando CACHE_TYPE=redis (Docker Compose, Kubernetes).
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(10))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("salas-abertas", config.entryTtl(TTL_SALAS))
                .enableStatistics() // Necessário para o Micrometer expor hits/misses
                .build();
    }

    /**
     * Cache em memória com TTL — fallback para ambientes sem Redis (ex.: Render).
     * Usa Caffeine em vez de ConcurrentMapCacheManager porque este último não
     * expira entradas, o que deixaria a lista de salas congelada.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("salas-abertas");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(TTL_SALAS.toSeconds(), TimeUnit.SECONDS)
                .maximumSize(100)
                .recordStats()); // Expõe hits/misses no Micrometer
        return manager;
    }
}
