package com.example.employeeservice.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EmployeeEventPublisher employeeEventPublisher;

    @Test
    void givenEmployeeEvent_whenPublish_thenSendToRabbitMQWithCorrectRoutingKey() {
        // Arrange: Create a sample employee created event
        EmployeeCreatedEvent event = new EmployeeCreatedEvent();
        event.setEmployeeId(100L);
        event.setFirstName("Jane");
        event.setLastName("Doe");
        event.setEmployeeEmail("jane.doe@example.com");
        event.setDepartment("Engineering");

        // Act: Call the publisher method
        employeeEventPublisher.publishEmployeeCreatedEvent(event);

        // Assert: Verify that RabbitTemplate was called with the exact Exchange, Routing Key, and Payload
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("leave.exchange"),
                eq("employee.created.key"),
                eq(event)
        );
    }

    @Test
    void givenRabbitMqDown_whenPublish_thenThrowAmqpException() {
        // Arrange: Create event
        EmployeeCreatedEvent event = new EmployeeCreatedEvent();
        event.setEmployeeId(101L);

        // Simulate RabbitMQ throwing a connection exception
        doThrow(new AmqpException("Broker Unreachable"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), eq(event));

        // Act & Assert: Verify that the exception propagates up so a fallback or retry can handle it
        assertThrows(AmqpException.class, () -> {
            employeeEventPublisher.publishEmployeeCreatedEvent(event);
        });
    }
}