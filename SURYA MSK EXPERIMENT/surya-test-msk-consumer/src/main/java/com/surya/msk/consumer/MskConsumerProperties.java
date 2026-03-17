package com.surya.msk.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

/** MSK IAM (9098) + local PLAINTEXT/SSL for Kafka consumer. */
public final class MskConsumerProperties {

    public static final String DEFAULT_TOPIC = "surya-test-topic";
    public static final String MSK_CLUSTER_NAME = "demo-cluster-1";

    private MskConsumerProperties() {}

    public static Properties forConsumer(String bootstrap, String groupId, String clientId) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                System.getenv().getOrDefault("KAFKA_AUTO_OFFSET_RESET", "earliest"));
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        p.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);

        if (useMskIam(bootstrap)) {
            String region = System.getenv().getOrDefault(
                    "AWS_REGION",
                    System.getenv().getOrDefault("KAFKA_AWS_REGION", "us-east-1"));
            p.put("security.protocol", "SASL_SSL");
            p.put("sasl.mechanism", "AWS_MSK_IAM");
            p.put("sasl.jaas.config",
                    "software.amazon.msk.auth.iam.IAMLoginModule required awsStsRegion=\"" + region + "\";");
            p.put("sasl.client.callback.handler.class",
                    "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
        } else {
            String protocol = System.getenv().getOrDefault("KAFKA_SECURITY_PROTOCOL", "").trim();
            if (!protocol.isEmpty()) {
                p.put("security.protocol", protocol);
            } else if (bootstrap != null && bootstrap.toLowerCase().contains("amazonaws.com")) {
                p.put("security.protocol", "SSL");
                p.put("ssl.endpoint.identification.algorithm", "https");
            } else {
                p.put("security.protocol", "PLAINTEXT");
            }
        }
        return p;
    }

    public static boolean useMskIam(String bootstrap) {
        if (bootstrap == null || bootstrap.isBlank()) return false;
        if ("true".equalsIgnoreCase(System.getenv().getOrDefault("MSK_USE_IAM", ""))) return true;
        return bootstrap.contains(":9098");
    }
}
