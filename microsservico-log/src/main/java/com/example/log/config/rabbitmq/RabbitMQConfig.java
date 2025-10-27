package com.example.log.config.rabbitmq;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.log.constants.TopicLog.*;

@Configuration
public class RabbitMQConfig {

    /**
     * Configuração do MessageConverter para serializar/deserializar mensagens JSON
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Define a fila para receber logs do microsserviço pessoa.
     * Esta fila recebe eventos de log para serem persistidos.
     *
     * @return Queue configurada para logs
     */
    @Bean
    public Queue logQueue() {
        return new Queue(TOPIC_ENVIAR_LOG, true);
    }

    /**
     * Configuração do listener container para consumir mensagens de log.
     * Permite controle manual de acknowledgment das mensagens.
     *
     * @return SimpleRabbitListenerContainerFactory configurado
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

}
