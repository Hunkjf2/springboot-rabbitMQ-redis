package com.example.log.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuração central do Redis para o microsserviço.
 * Define serialização, templates e estratégias de cache.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate customizado para operações no Redis.
     *
     * Estratégia de Serialização:
     * - Chaves: String (legibilidade e compatibilidade)
     * - Valores: JSON (flexibilidade e suporte a objetos complexos)
     *
     * @param connectionFactory Fábrica de conexões gerenciada pelo Spring
     * @param objectMapper ObjectMapper configurado (reutiliza config existente)
     * @return RedisTemplate configurado
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serializer para chaves (String simples)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Serializer para valores (JSON com ObjectMapper customizado)
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // Configuração de serialização
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Habilita transações (opcional, para operações atômicas)
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

}