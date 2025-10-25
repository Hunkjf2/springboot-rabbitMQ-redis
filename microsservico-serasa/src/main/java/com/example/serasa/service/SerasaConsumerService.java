package com.example.serasa.service;

import com.example.serasa.config.exception.SerasaMessageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;
import static com.example.serasa.constants.TopicSerasa.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class SerasaConsumerService {

    private final SerasaService serasaService;

    @KafkaListener(topics = TOPIC_VERIFICAR_SERASA_REQUEST)
    @SendTo(TOPIC_VERIFICAR_SERASA_RESPONSE)
    public String consultarCpf(String cpf, Acknowledgment acknowledgment) {
        try {

            boolean resultado = serasaService.consultarCpfSerasa(cpf);

            log.info("Resultado da consulta para CPF {}: {}", cpf, resultado);
            acknowledgment.acknowledge();
            return String.valueOf(resultado);
        } catch (Exception e) {
            throw new SerasaMessageException("Falha no processamento:", e);
        }
    }

}