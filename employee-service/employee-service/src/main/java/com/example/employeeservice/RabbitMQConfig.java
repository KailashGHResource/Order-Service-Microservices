package com.example.employeeservice;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // The Employee Service only needs a JSON Message Converter to publish events.
    // Exchanges and Queues are owned and declared by the respective consumer microservices
    // (Notification Service and Audit Service).

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}