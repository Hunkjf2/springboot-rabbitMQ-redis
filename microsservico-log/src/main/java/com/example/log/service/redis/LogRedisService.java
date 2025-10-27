package com.example.log.service.redis;

import com.example.log.config.redis.RedisProperties;
import com.example.log.constants.RedisKeys;
import com.example.log.model.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço especializado para operações de Log no Redis.
 * Implementa lógica de negócio específica para caching de logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogRedisService {

    private final RedisService redisService;
    private final RedisProperties redisProperties;
    private final ObjectMapper objectMapper;

    /**
     * Salva um log no Redis.
     * Estratégia: Salva individualmente + adiciona à lista geral.
     *
     * @param logPayload Entidade Log a ser cacheada
     */
    public void saveLog(Log logPayload) {
        try {
            // 1. Salva log individual com chave: "log:ID"
            String logKey = buildLogKey(logPayload.getId());
            redisService.save(logKey, logPayload, redisProperties.getDefaultTtl());

            // 2. Adiciona ID à lista de todos os logs
            String allLogsKey = buildAllLogsKey();
            redisService.addToList(allLogsKey, logPayload.getId());

            // 3. Define TTL na lista (renovado a cada adição)
            redisService.expire(allLogsKey, redisProperties.getListTtl());

            // 4. Adiciona a índices secundários (operação e serviço)
            addToIndexes(logPayload);

            log.info("Log cacheado no Redis: id={}, operacao={}",
                    logPayload.getId(), logPayload.getOperacao());

        } catch (Exception e) {
            log.error("Erro ao salvar log no Redis: id={}", logPayload.getId(), e);
        }
    }

    /**
     * Busca um log individual por ID.
     *
     * @param id ID do log
     * @return Log encontrado ou null
     */
    public Log getLogById(Long id) {
        try {
            String key = buildLogKey(id);
            Object cached = redisService.get(key);

            if (cached != null) {
                // LinkedHashMap precisa ser convertido para Log
                return objectMapper.convertValue(cached, Log.class);
            }

            log.debug("Cache miss para log id={}", id);
            return null;

        } catch (Exception e) {
            log.error("Erro ao buscar log do Redis: id={}", id, e);
            return null;
        }
    }

    /**
     * Busca todos os logs do cache.
     * Retorna lista limitada para performance.
     *
     * @return Lista de logs do Redis
     */
    public List<Log> getAllLogs() {
        try {
            String allLogsKey = buildAllLogsKey();

            // Busca lista de IDs (limitado por maxListSize)
            List<Object> ids = redisService.getList(
                    allLogsKey,
                    0,
                    redisProperties.getMaxListSize() - 1
            );

            if (ids == null || ids.isEmpty()) {
                log.debug("Nenhum log encontrado no cache");
                return List.of();
            }

            // Busca cada log individualmente
            List<Log> logs = new ArrayList<>();
            for (Object idObj : ids) {
                Long id = objectMapper.convertValue(idObj, Long.class);
                Log log = getLogById(id);
                if (log != null) {
                    logs.add(log);
                }
            }

            log.info("Retornados {} logs do cache Redis", logs.size());
            return logs;

        } catch (Exception e) {
            log.error("Erro ao buscar todos os logs do Redis", e);
            return List.of();
        }
    }

    /**
     * Busca logs por operação (CADASTRO, ATUALIZAÇÃO, EXCLUSÃO).
     *
     * @param operacao Tipo de operação
     * @return Lista de logs filtrados
     */
    public List<Log> getLogsByOperacao(String operacao) {
        try {
            String key = RedisKeys.LOGS_BY_OPERATION + operacao;
            List<Object> ids = redisService.getList(key, 0, -1);

            return ids.stream()
                    .map(id -> getLogById(objectMapper.convertValue(id, Long.class)))
                    .filter(log -> log != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erro ao buscar logs por operação: {}", operacao, e);
            return List.of();
        }
    }

    /**
     * Adiciona log aos índices secundários.
     * Facilita buscas por operação e microsserviço.
     *
     * @param logPayload Log a ser indexado
     */
    private void addToIndexes(Log logPayload) {
        try {
            // Índice por operação
            String operationKey = RedisKeys.LOGS_BY_OPERATION + logPayload.getOperacao();
            redisService.addToList(operationKey, logPayload.getId());
            redisService.expire(operationKey, redisProperties.getListTtl());

            // Índice por microsserviço
            String serviceKey = RedisKeys.LOGS_BY_SERVICE + logPayload.getNomeMicroSservico();
            redisService.addToList(serviceKey, logPayload.getId());
            redisService.expire(serviceKey, redisProperties.getListTtl());

        } catch (Exception e) {
            log.error("Erro ao adicionar log aos índices: id={}", logPayload.getId(), e);
        }
    }

    /**
     * Constrói chave individual de log.
     *
     * @param id ID do log
     * @return Chave formatada (ex: "log:123")
     */
    private String buildLogKey(Long id) {
        return redisProperties.getKeyPrefix() + RedisKeys.LOG_PREFIX + id;
    }

    /**
     * Constrói chave da lista de todos os logs.
     *
     * @return Chave formatada (ex: "log:all")
     */
    private String buildAllLogsKey() {
        return redisProperties.getKeyPrefix() + RedisKeys.ALL_LOGS_LIST;
    }

}