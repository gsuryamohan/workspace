package com.surya.msk.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * Minimal MSK / Kafka consumer. Set bootstrap servers via env KAFKA_BOOTSTRAP_SERVERS
 * or system property kafka.bootstrap.servers.
 */
public final class MskConsumerApp {

    public static void main(String[] args) {
        String bootstrap = System.getenv().getOrDefault(
                "KAFKA_BOOTSTRAP_SERVERS",
                System.getProperty("kafka.bootstrap.servers", "localhost:9092"));
        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", "surya-test-topic");
        String groupId = System.getenv().getOrDefault("KAFKA_GROUP_ID", "surya-test-msk-consumer");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "surya-test-msk-consumer");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            System.out.printf("Subscribed to %s (group=%s). Polling... Ctrl+C to stop.%n", topic, groupId);
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                records.forEach(r ->
                        System.out.printf("partition=%d offset=%d key=%s value=%s%n",
                                r.partition(), r.offset(), r.key(), r.value()));
            }
        }
    }
}
