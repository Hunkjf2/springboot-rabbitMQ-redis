package com.example.pessoa.config.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import java.util.Map;
import static com.example.pessoa.constants.serasa.TopicSerasa.*;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.producer.retries}")
    private String retriesConfig;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;


    // Assincrona
    /**
     * Configuração do producer
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.RETRIES_CONFIG, retriesConfig, // Para reenviar mensagens que falharam
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false // Para evitar duplicação de mensagens
        ));
    }

    /**
     * Cria um KafkaTemplate para envio de mensagens assíncronas.
     *
     * @return Template configurado para envio de mensagens
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    // Sincrona
    /**
     * Cria um ConsumerFactory para comunicação síncrona (request-reply).
     * Configura o consumer com commit manual para controle preciso
     * sobre quando as mensagens são confirmadas como processadas.
     *
     * @return ConsumerFactory configurado para replies
     */
    @Bean
    public ConsumerFactory<String, String> replyConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset, // Para garantir que as mensagens sejam lidas desde o início se não houver marcarção (offset)
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false // Desabilita a confirmação de processamento automático
        ));
    }

    /**
     * Define o modo de confirmação (ACK) como MANUAL_IMMEDIATE.
     * Neste modo, o offset é confirmado imediatamente após o processamento manual
     * da mensagem, garantindo controle preciso sobre quando as mensagens são
     * consideradas como processadas com sucesso.
     *
     * @return Container configurado para escutar respostas
     */
    @Bean
    public ConcurrentMessageListenerContainer<String, String> replyContainer() {
        ContainerProperties containerProperties = new ContainerProperties(TOPIC_VERIFICAR_SERASA_RESPONSE);
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return new ConcurrentMessageListenerContainer<>(replyConsumerFactory(), containerProperties);
    }

    /**
     * Cria um ReplyingKafkaTemplate para comunicação síncrona request-reply.
     * Este template permite enviar uma mensagem e aguardar uma resposta.
     *
     * @return Template configurado para comunicação síncrona
     */
    @Bean
    public ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate() {
        return new ReplyingKafkaTemplate<>(producerFactory(), replyContainer());
    }

}
