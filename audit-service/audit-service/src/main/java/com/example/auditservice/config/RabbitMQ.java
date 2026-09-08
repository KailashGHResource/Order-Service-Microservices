package com.example.auditservice.config; // Replace with your actual config package

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQ {

    public static final String EXCHANGE_NAME = "leave.exchange";

    // Employee constants
    public static final String AUDIT_QUEUE = "audit.employee.created.queue";
    public static final String ROUTING_KEY = "employee.created.key";

    // --- NEW: Leave constants ---
    public static final String AUDIT_LEAVE_QUEUE = "audit.leave.applied.queue";
    public static final String LEAVE_ROUTING_KEY = "leave.applied.key";

    // --- Dead Letter Infrastructure ---
    public static final String DLX_NAME = "leave.dlx";
    public static final String DLQ_NAME = "leave.dlq";
    public static final String DLQ_ROUTING_KEY = "leave.dlq.routing.key";

    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    // 1. Employee Queue & Binding
    @Bean
    public Queue auditEmployeeCreatedQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding auditBinding(Queue auditEmployeeCreatedQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(auditEmployeeCreatedQueue).to(leaveExchange).with(ROUTING_KEY);
    }

    // --- NEW: 2. Leave Queue & Binding ---
    @Bean
    public Queue auditLeaveAppliedQueue() {
        return QueueBuilder.durable(AUDIT_LEAVE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding auditLeaveBinding(Queue auditLeaveAppliedQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(auditLeaveAppliedQueue).to(leaveExchange).with(LEAVE_ROUTING_KEY);
    }

    // 3. JSON Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }
}