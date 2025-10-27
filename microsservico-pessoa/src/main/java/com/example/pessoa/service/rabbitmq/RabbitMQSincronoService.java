package com.example.pessoa.service.rabbitmq;

import com.example.pessoa.config.exception.ProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RabbitMQSincronoService {

    private final RabbitTemplate rabbitTemplate;
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    public String enviarEReceber(String queue, String cpf) {
        try {
            rabbitTemplate.setReplyTimeout(TIMEOUT.toMillis());

            Object response = rabbitTemplate.convertSendAndReceive(queue, cpf);

            if (response == null) {
                throw new ProcessingException("Timeout ou nenhuma resposta recebida", null);
            }

            return response.toString();
        } catch (Exception e) {
            throw new ProcessingException("Falha no envio e recebimento da mensagem:", e);
        }
    }

}
