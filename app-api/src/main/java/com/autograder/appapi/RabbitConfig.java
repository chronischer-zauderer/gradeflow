package com.autograder.appapi;

import com.autograder.shared.Queues;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    Queue submissionsQueue() {
        return QueueBuilder.durable(Queues.SUBMISSIONS).build();
    }
}
