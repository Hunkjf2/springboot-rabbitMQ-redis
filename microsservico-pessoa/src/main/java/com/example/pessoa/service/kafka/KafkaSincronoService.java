package com.example.pessoa.service.kafka;

import com.example.pessoa.config.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KafkaSincronoService {

    private final ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate;
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    public String enviarEReceber(String topic, String cpf) {
        try {

            Message<String> message = MessageBuilder
                    .withPayload(cpf)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .build();

            var future = replyingKafkaTemplate.sendAndReceive(message, TIMEOUT);
            var response = future.get();

            return (String) response.getPayload();
        } catch (Exception e) {
            throw new ProcessingException("Falha no envio e recebimento da mensagem:", e);
        }
    }

}
