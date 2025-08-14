package com.beta.apiservice.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic topic(@Value("${app.kafka.topic:notifyx_test}") String topic) {
        return new NewTopic(topic, 1, (short) 1);
    }
}


