package com.example.leaveservice.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeaveEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 1. Existing Approved Event Publisher
    public void publishLeaveApprovedEvent(LeaveApprovedEvent event) {
        rabbitTemplate.convertAndSend("leave.exchange", "leave.approved.key", event);
        System.out.println("Published LeaveApprovedEvent to RabbitMQ: " + event.getLeaveId());
    }

    // 2. New Applied Event Publisher
    public void publishLeaveAppliedEvent(LeaveAppliedEvent event) {
        // Sends the event using the new APPLIED routing key
        rabbitTemplate.convertAndSend("leave.exchange", "leave.applied.key", event);
        System.out.println("Published LeaveAppliedEvent to RabbitMQ: " + event.getLeaveId());
    }

    // 3. New Rejected Event Publisher
    public void publishLeaveRejectedEvent(LeaveRejectedEvent event) {
        // Sends the event using the new REJECTED routing key
        rabbitTemplate.convertAndSend("leave.exchange", "leave.rejected.key", event);
        System.out.println("Published LeaveRejectedEvent to RabbitMQ: " + event.getLeaveId());
    }
}