package com.example.auditservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQ {

    public static final String EXCHANGE_NAME = "leave.exchange";
    public static final String AUDIT_QUEUE = "audit.employee.created.queue";
    public static final String ROUTING_KEY = "employee.created.key";

    // --- Dead Letter Infrastructure ---
    public static final String DLX_NAME = "leave.dlx";
    public static final String DLQ_NAME = "leave.dlq";
    public static final String DLQ_ROUTING_KEY = "leave.dlq.routing.key";

    // 1. Declare the Core Exchange & DLX
    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    // 2. Declare the Audit Queue with DLX Linking
    @Bean
    public Queue auditEmployeeCreatedQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    // 3. Bind the Audit Queue to the Exchange using the Routing Key
    @Bean
    public Binding auditBinding(Queue auditEmployeeCreatedQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(auditEmployeeCreatedQueue)
                .to(leaveExchange)
                .with(ROUTING_KEY);
    }

    // 4. JSON Message Converter (UPDATED)
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        // This is the magic line. It forces RabbitMQ to ignore the sender's class package string
        // and safely infers the type directly from your @RabbitListener's method signature.
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);

        return converter;
    }
}