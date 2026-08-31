package com.example.notificationservice;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "leave.exchange";

    // --- Dead Letter Infrastructure ---
    public static final String DLX_NAME = "leave.dlx";
    public static final String DLQ_NAME = "leave.dlq";
    public static final String DLQ_ROUTING_KEY = "leave.dlq.routing.key";

    // --- COMPENSATION INFRASTRUCTURE ---
    public static final String COMPENSATION_QUEUE = "leave.compensation.queue";
    public static final String COMPENSATION_ROUTING_KEY = "leave.compensation.key";

    // --- Queue Names ---
    public static final String APPROVED_QUEUE = "notification.leave.queue";
    public static final String APPLIED_QUEUE = "notification.leave.applied.queue";
    public static final String REJECTED_QUEUE = "notification.leave.rejected.queue";

    // --- DAY 3 EMPLOYEE ONBOARDING INFRASTRUCTURE ---
    public static final String EMPLOYEE_CREATED_QUEUE = "notification.employee.created.queue";
    public static final String EMPLOYEE_CREATED_ROUTING_KEY = "employee.created.key";

    // --- Routing Keys ---
    public static final String APPROVED_ROUTING_KEY = "leave.approved.key";
    public static final String APPLIED_ROUTING_KEY = "leave.applied.key";
    public static final String REJECTED_ROUTING_KEY = "leave.rejected.key";

    // 1. Core Exchange & DLX Exchange
    @Bean
    public TopicExchange leaveExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    // 2. The Dead Letter Queue & Binding
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ_NAME, true);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    // 3. Main Queues (Linked to the DLX)
    @Bean
    public Queue leaveApprovedQueue() {
        return QueueBuilder.durable(APPROVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue leaveAppliedQueue() {
        return QueueBuilder.durable(APPLIED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue leaveRejectedQueue() {
        return QueueBuilder.durable(REJECTED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    // --- EMPLOYEE CREATED QUEUE & DLX LINKING ---
    @Bean
    public Queue employeeCreatedQueue() {
        return QueueBuilder.durable(EMPLOYEE_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    // 4. Bind the Main Queues to the Main Exchange
    @Bean
    public Binding bindingApproved(Queue leaveApprovedQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(leaveApprovedQueue).to(leaveExchange).with(APPROVED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingApplied(Queue leaveAppliedQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(leaveAppliedQueue).to(leaveExchange).with(APPLIED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingRejected(Queue leaveRejectedQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(leaveRejectedQueue).to(leaveExchange).with(REJECTED_ROUTING_KEY);
    }

    // --- BIND EMPLOYEE CREATED QUEUE ---
    @Bean
    public Binding bindingEmployeeCreated(Queue employeeCreatedQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(employeeCreatedQueue).to(leaveExchange).with(EMPLOYEE_CREATED_ROUTING_KEY);
    }

    // 5. Compensation Queue & Binding
    @Bean
    public Queue leaveCompensationQueue() {
        return new Queue(COMPENSATION_QUEUE, true);
    }

    @Bean
    public Binding bindingCompensation(Queue leaveCompensationQueue, TopicExchange leaveExchange) {
        return BindingBuilder.bind(leaveCompensationQueue).to(leaveExchange).with(COMPENSATION_ROUTING_KEY);
    }

    // 6. Message Converter (Translates Java Objects to JSON)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}