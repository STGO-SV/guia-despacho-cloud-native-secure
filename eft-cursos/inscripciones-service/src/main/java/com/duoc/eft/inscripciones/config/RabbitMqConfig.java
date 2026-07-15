package com.duoc.eft.inscripciones.config;

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
    @Bean DirectExchange cursosExchange(@Value("${app.rabbit.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }
    @Bean Queue inscripcionesQueue(@Value("${app.rabbit.main-queue}") String name,
            @Value("${app.rabbit.exchange}") String exchange,
            @Value("${app.rabbit.error-routing-key}") String errorKey) {
        return new Queue(name, true, false, false, Map.of(
                "x-dead-letter-exchange", exchange, "x-dead-letter-routing-key", errorKey));
    }
    @Bean Queue inscripcionesErrorQueue(@Value("${app.rabbit.error-queue}") String name) {
        return new Queue(name, true, false, false);
    }
    @Bean Binding mainBinding(@Qualifier("inscripcionesQueue") Queue queue, DirectExchange cursosExchange,
            @Value("${app.rabbit.created-routing-key}") String key) {
        return BindingBuilder.bind(queue).to(cursosExchange).with(key);
    }
    @Bean Binding errorBinding(@Qualifier("inscripcionesErrorQueue") Queue queue, DirectExchange cursosExchange,
            @Value("${app.rabbit.error-routing-key}") String key) {
        return BindingBuilder.bind(queue).to(cursosExchange).with(key);
    }
    @Bean MessageConverter messageConverter(ObjectMapper mapper) { return new Jackson2JsonMessageConverter(mapper); }
    @Bean RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(factory); template.setMessageConverter(converter);
        template.setMandatory(true); return template;
    }
    @Bean RetryOperationsInterceptor retryInterceptor(
            @Value("${app.rabbit.retry.max-attempts:3}") int attempts) {
        return RetryInterceptorBuilder.stateless().maxAttempts(attempts)
                .backOffOptions(500, 2.0, 2000).recoverer(new RejectAndDontRequeueRecoverer()).build();
    }
    @Bean SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory factory,
            MessageConverter converter, RetryOperationsInterceptor retryInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {
        SimpleRabbitListenerContainerFactory result = new SimpleRabbitListenerContainerFactory();
        result.setConnectionFactory(factory); result.setMessageConverter(converter);
        result.setDefaultRequeueRejected(false); result.setAdviceChain(retryInterceptor);
        result.setAutoStartup(autoStartup); return result;
    }
}

