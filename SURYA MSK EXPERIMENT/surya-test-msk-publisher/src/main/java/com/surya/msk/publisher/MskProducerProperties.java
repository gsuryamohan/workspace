package com.surya.msk.publisher;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Producer properties for Amazon MSK {@code demo-cluster-1} (IAM on port 9098) and local dev.
 * <p>
 * Target: topic {@value #DEFAULT_TOPIC} on cluster {@value #MSK_CLUSTER_NAME}.
 */
public final class MskProducerProperties {

    public static final String MSK_CLUSTER_NAME = "demo-cluster-1";
    public static final String DEFAULT_TOPIC = "surya-test-topic";

    private MskProducerProperties() {}

    /**
     * Builds producer config. Uses MSK IAM when {@code MSK_USE_IAM=true} or bootstrap uses port 9098.
     *
     * @param bootstrap bootstrap servers (IAM brokers for demo-cluster-1 end with {@code :9098})
     * @param clientId  Kafka client id
     */
    public static Properties forProducer(String bootstrap, String clientId) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);

        boolean useIam = useMskIam(bootstrap);
        if (useIam) {
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

    /** IAM when explicitly enabled or bootstrap uses MSK IAM port. */
    public static boolean useMskIam(String bootstrap) {
        if (bootstrap == null || bootstrap.isBlank()) {
            return false;
        }
        String flag = System.getenv().getOrDefault("MSK_USE_IAM", "");
        if ("true".equalsIgnoreCase(flag) || "1".equals(flag)) {
            return true;
        }
        // demo-cluster-1 bootstrap brokers use 9098
        if (bootstrap.contains(":9098")) {
            return true;
        }
        return false;
    }
}
