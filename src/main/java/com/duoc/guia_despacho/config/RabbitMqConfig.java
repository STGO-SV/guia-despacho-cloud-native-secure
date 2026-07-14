package com.duoc.guia_despacho.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMqConfig {

    @Bean
    DirectExchange guiaExchange(@Value("${app.rabbit.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    Queue guiaProcesamientoQueue(
            @Value("${app.rabbit.main-queue}") String queue,
            @Value("${app.rabbit.exchange}") String exchange,
            @Value("${app.rabbit.error-routing-key}") String errorRoutingKey
    ) {
        return new Queue(queue, true, false, false, Map.of(
                "x-dead-letter-exchange", exchange,
                "x-dead-letter-routing-key", errorRoutingKey
        ));
    }

    @Bean
    Queue guiaErrorQueue(@Value("${app.rabbit.error-queue}") String queue) {
        return new Queue(queue, true, false, false);
    }

    @Bean
    Binding guiaCreadaBinding(
            @Qualifier("guiaProcesamientoQueue") Queue queue,
            DirectExchange guiaExchange,
            @Value("${app.rabbit.created-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(queue).to(guiaExchange).with(routingKey);
    }

    @Bean
    Binding guiaErrorBinding(
            @Qualifier("guiaErrorQueue") Queue queue,
            DirectExchange guiaExchange,
            @Value("${app.rabbit.error-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(queue).to(guiaExchange).with(routingKey);
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    RetryOperationsInterceptor rabbitRetryInterceptor(
            @Value("${app.rabbit.retry.max-attempts}") int maxAttempts,
            @Value("${app.rabbit.retry.initial-interval}") long initialInterval,
            @Value("${app.rabbit.retry.multiplier}") double multiplier,
            @Value("${app.rabbit.retry.max-interval}") long maxInterval
    ) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(initialInterval, multiplier, maxInterval)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            RetryOperationsInterceptor rabbitRetryInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        factory.setAdviceChain(rabbitRetryInterceptor);
        return factory;
    }
}
