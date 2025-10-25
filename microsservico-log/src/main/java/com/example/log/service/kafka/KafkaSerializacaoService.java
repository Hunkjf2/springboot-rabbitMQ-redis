package com.example.log.service.kafka;

import com.example.log.config.exception.ProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaSerializacaoService {

    private final ObjectMapper objectMapper;

    public <T> T deserialize(String payload, Class<T> targetType) {
        try {
            return objectMapper.readValue(payload, targetType);
        } catch (Exception e) {
            throw new ProcessingException("Falha na deserialização", e);
        }
    }

}