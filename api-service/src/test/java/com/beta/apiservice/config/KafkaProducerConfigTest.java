package com.beta.apiservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
class KafkaProducerConfigTest {
    @Autowired
    private ProducerFactory<String, String> producerFactory;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void producerFactoryBeanExists() {
        assertThat(producerFactory).isNotNull();
    }

    @Test
    void kafkaTemplateBeanExists() {
        assertThat(kafkaTemplate).isNotNull();
    }
} 