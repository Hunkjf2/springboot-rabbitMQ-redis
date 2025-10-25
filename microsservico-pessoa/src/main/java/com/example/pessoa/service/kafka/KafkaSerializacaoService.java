package com.example.pessoa.service.kafka;

import com.example.pessoa.config.exception.ProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaSerializacaoService {

    private final ObjectMapper objectMapper;

    public String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ProcessingException("Falha na serialização", e);
        }
    }

}