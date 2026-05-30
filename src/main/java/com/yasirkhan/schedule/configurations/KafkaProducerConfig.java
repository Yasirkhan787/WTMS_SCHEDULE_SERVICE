package com.yasirkhan.schedule.configurations;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private final String BOOTSTRAP_SERVER;

    public KafkaProducerConfig(@Value("${kafka.bootstrap.server}") String bootstrapServer) {
        BOOTSTRAP_SERVER = bootstrapServer;
    }

    // --- DLTs for Schedule Service Consumers ---
    @Bean
    public NewTopic userEventDLT() { return new NewTopic("user-response-topic.DLT", 2, (short) 1); }
    @Bean
    public NewTopic vehicleEventDLT() { return new NewTopic("vehicle-response-topic.DLT", 2, (short) 1); }
    @Bean
    public NewTopic routeEventDLT() { return new NewTopic("route-response-topic.DLT", 2, (short) 1); }

    @Bean
    public Map<String, Object> producerConfig(){
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        return properties;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }
}