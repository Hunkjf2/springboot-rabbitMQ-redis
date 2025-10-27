package com.example.log.controller;

import com.example.log.dto.LogResponseDto;
import com.example.log.model.Log;
import com.example.log.service.log.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para operações de consulta de logs.
 * Todas as consultas utilizam Redis como fonte primária.
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Logs", description = "Gerenciamento de logs de auditoria")
public class LogController {

    private final LogService logService;

    /**
     * Endpoint: GET /logs
     * Busca todos os logs (Redis primeiro, fallback PostgreSQL).
     *
     * @return Lista de logs
     */
    @GetMapping
    @Operation(summary = "Listar todos os logs",
            description = "Retorna todos os logs do sistema. Dados vêm do cache Redis para performance otimizada.")
    public ResponseEntity<List<LogResponseDto>> listarTodos() {
        log.info("Requisição recebida: GET /logs");

        List<Log> logs = logService.buscarTodosLogs();

        List<LogResponseDto> response = logs.stream()
                .map(LogResponseDto::from)
                .collect(Collectors.toList());

        log.info("Retornando {} logs", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint: GET /logs/operacao/{operacao}
     * Busca logs por tipo de operação.
     *
     * @param operacao Operação (CADASTRO, ATUALIZAÇÃO, EXCLUSÃO)
     * @return Lista filtrada de logs
     */
    @GetMapping("/operacao/{operacao}")
    @Operation(summary = "Buscar logs por operação",
            description = "Filtra logs por tipo de operação (CADASTRO, ATUALIZAÇÃO, EXCLUSÃO)")
    public ResponseEntity<List<LogResponseDto>> buscarPorOperacao(
            @PathVariable String operacao) {

        log.info("Requisição recebida: GET /logs/operacao/{}", operacao);

        List<Log> logs = logService.buscarLogsPorOperacao(operacao.toUpperCase());

        List<LogResponseDto> response = logs.stream()
                .map(LogResponseDto::from)
                .collect(Collectors.toList());

        log.info("Retornando {} logs para operação {}", response.size(), operacao);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint: GET /logs/health
     * Health check do serviço.
     *
     * @return Status do serviço
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o serviço está operacional")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Log Service is UP");
    }

}