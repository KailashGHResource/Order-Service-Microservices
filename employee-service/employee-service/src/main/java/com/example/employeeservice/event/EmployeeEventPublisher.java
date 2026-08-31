package com.example.employeeservice.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishEmployeeCreatedEvent(EmployeeCreatedEvent event) {
        // ---> FIXED: Reverted back to 'leave.exchange' to match your Notification & Audit config <---
        rabbitTemplate.convertAndSend("leave.exchange", "employee.created.key", event);
        System.out.println("Published EmployeeCreatedEvent to RabbitMQ for Employee ID: " + event.getEmployeeId());
    }
}