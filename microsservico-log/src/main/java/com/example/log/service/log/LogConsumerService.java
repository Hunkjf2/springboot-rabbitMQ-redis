package com.example.log.service.log;

import com.example.log.config.exception.ProcessingException;
import com.example.log.dto.LogEventDto;
import com.example.log.dto.PessoaDto;
import com.example.log.model.Log;
import com.example.log.service.kafka.KafkaSerializacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import static com.example.log.constants.TopicLog.*;

@Service
@RequiredArgsConstructor
public class LogConsumerService {
    
    private final LogService logService;
    private final KafkaSerializacaoService kafkaSerializacaoService;

    @KafkaListener(
        topics = TOPIC_ENVIAR_LOG
    )
    public void processarEnvioLog(String mensagem, Acknowledgment acknowledgment) {
        try {

            LogEventDto logEventDto = kafkaSerializacaoService.deserialize(mensagem, LogEventDto.class);
            PessoaDto pessoaDto = logEventDto.pessoaDto();

            Log log = Log.builder()
                    .operacao(logEventDto.operacao())
                    .dados(mensagem)
                    .dataHoraCriacao(pessoaDto.dataHoraCriacao())
                    .nomeUsuario(logEventDto.nomeUsuario())
                    .nomeMicroSservico(logEventDto.microservico())
                    .idUsuario(pessoaDto.id())
                    .build();

            logService.cadastrarLog(log);

            acknowledgment.acknowledge(); // Confirma o processamento da mensagem
        } catch (Exception e) {
            throw new ProcessingException("Erro ao processar mensagem de cadastro de log: {}", e);
        }

    }
    
}