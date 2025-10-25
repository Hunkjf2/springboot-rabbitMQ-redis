package com.example.pessoa.service.kafka;

import com.example.pessoa.config.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaAssincronoService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaSerializacaoService kafkaSerializacaoService;

    public void enviar(String topic, Object payload) {
        try {
            String stringJson = kafkaSerializacaoService.serialize(payload);
            kafkaTemplate.send(topic, stringJson);
            log.info("Mensagem enviada para tópico: {} Mensagem: {} ", topic, stringJson);
        } catch (Exception e) {
            throw new ProcessingException("Falha no envio do evento:", e);
        }
    }
}