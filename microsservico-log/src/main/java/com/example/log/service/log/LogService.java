package com.example.log.service.log;

import com.example.log.model.Log;
import com.example.log.repository.LogRepository;
import com.example.log.service.redis.LogRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Serviço de negócio para Logs.
 * Orquestra operações entre PostgreSQL (persistência) e Redis (cache).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;
    private final LogRedisService logRedisService;

    /**
     * Cadastra log no PostgreSQL e Redis.
     * Padrão Write-Through: Escreve em ambos simultaneamente.
     *
     * @param log Entidade a ser persistida
     */
    @Transactional
    public void cadastrarLog(Log logPayload) {
        try {
            // 1. Salva no banco de dados (source of truth)
            Log logSalvo = logRepository.save(logPayload);
            log.info("Log salvo no PostgreSQL: id={}", logSalvo.getId());

            // 2. Salva no Redis (cache/consulta rápida)
            logRedisService.saveLog(logSalvo);

        } catch (Exception e) {
            log.error("Erro ao cadastrar log", e);
            throw e; // Propaga para tratamento upstream
        }
    }

    /**
     * Busca todos os logs.
     * Estratégia Cache-Aside: Tenta Redis primeiro, fallback para PostgreSQL.
     *
     * @return Lista de logs
     */
    public List<Log> buscarTodosLogs() {
        try {
            // 1. Tenta buscar do Redis (rápido)
            List<Log> logsFromCache = logRedisService.getAllLogs();

            if (!logsFromCache.isEmpty()) {
                log.info("Logs retornados do cache Redis: {} registros", logsFromCache.size());
                return logsFromCache;
            }

            // 2. Fallback: Busca do PostgreSQL
            log.info("Cache miss - buscando logs do PostgreSQL");
            List<Log> logsFromDb = logRepository.findAll();

            // 3. Popula cache para próximas requisições
            if (!logsFromDb.isEmpty()) {
                logsFromDb.forEach(logRedisService::saveLog);
                log.info("Cache populado com {} logs do PostgreSQL", logsFromDb.size());
            }

            return logsFromDb;

        } catch (Exception e) {
            log.error("Erro ao buscar logs", e);
            // Em caso de erro total, retorna lista vazia para não quebrar API
            return List.of();
        }
    }

    /**
     * Busca logs por operação.
     *
     * @param operacao Tipo de operação (CADASTRO, ATUALIZAÇÃO, EXCLUSÃO)
     * @return Lista filtrada de logs
     */
    public List<Log> buscarLogsPorOperacao(String operacao) {
        try {
            // Tenta do Redis
            List<Log> logsFromCache = logRedisService.getLogsByOperacao(operacao);

            if (!logsFromCache.isEmpty()) {
                return logsFromCache;
            }

            // Fallback para PostgreSQL
            return logRepository.findAll().stream()
                    .filter(log -> operacao.equals(log.getOperacao()))
                    .toList();

        } catch (Exception e) {
            log.error("Erro ao buscar logs por operação: {}", operacao, e);
            return List.of();
        }
    }

}