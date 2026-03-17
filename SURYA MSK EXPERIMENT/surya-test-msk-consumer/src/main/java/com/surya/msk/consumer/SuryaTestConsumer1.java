package com.surya.msk.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;

/**
 * Surya-Test-Consumer-1: MSK demo-cluster-1, topic surya-test-topic, IAM bootstrap :9098.
 */
public final class SuryaTestConsumer1 {

    private static final Logger log = LoggerFactory.getLogger(SuryaTestConsumer1.class);
    public static final String MSK_CLUSTER_NAME = "demo-cluster-1";

    public static void main(String[] args) {
        String bootstrap = System.getenv().getOrDefault(
                "KAFKA_BOOTSTRAP_SERVERS",
                System.getProperty("kafka.bootstrap.servers", ""));
        if (bootstrap == null || bootstrap.isBlank()) {
            System.err.println("Set KAFKA_BOOTSTRAP_SERVERS (IAM 9098 for demo-cluster-1).");
            System.exit(1);
        }

        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", MskConsumerProperties.DEFAULT_TOPIC);
        String groupId = System.getenv().getOrDefault(
                "KAFKA_GROUP_ID",
                "surya-demo-cluster-1-Surya-Test-Consumer-1");
        Properties props = MskConsumerProperties.forConsumer(bootstrap, groupId, "Surya-Test-Consumer-1");

        System.out.println("[consumer] starting mskCluster=" + MSK_CLUSTER_NAME + " topic=" + topic
                + " groupId=" + groupId + " IAM=" + MskConsumerProperties.useMskIam(bootstrap));
        System.out.flush();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            System.out.println("[consumer] subscribed " + topic + "; polling…");
            System.out.flush();

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> r : records) {
                    logConsumedEvent(r);
                }
            }
        }
    }

    private static void logConsumedEvent(ConsumerRecord<String, String> r) {
        String value = r.value() != null ? r.value() : "";
        String preview = value.length() > 200 ? value.substring(0, 200) + "…" : value;
        String line = String.format("[CONSUMED] ts=%s partition=%d offset=%d key=%s len=%d payload=%s",
                Instant.now(), r.partition(), r.offset(), r.key(), value.length(), preview);
        System.out.println(line);
        System.out.flush();
        log.info("Consumed | partition={} offset={} len={}", r.partition(), r.offset(), value.length());
    }
}
