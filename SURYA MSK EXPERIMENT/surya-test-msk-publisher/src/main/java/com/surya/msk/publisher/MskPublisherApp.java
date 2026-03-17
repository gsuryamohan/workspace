package com.surya.msk.publisher;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * Publishes to Amazon MSK <strong>demo-cluster-1</strong>, topic <strong>surya-test-topic</strong>
 * (defaults). Uses MSK IAM when {@code KAFKA_BOOTSTRAP_SERVERS} uses port 9098 or {@code MSK_USE_IAM=true}.
 * <p>
 * Required for MSK: {@code KAFKA_BOOTSTRAP_SERVERS} (IAM bootstrap from MSK console / CLI).
 * Credentials: instance profile, env keys, or {@code ~/.aws/credentials} for IAM {@code kafka-cluster:Connect}
 * on the cluster resource.
 */
public final class MskPublisherApp {

    public static void main(String[] args) {
        String bootstrap = System.getenv().getOrDefault(
                "KAFKA_BOOTSTRAP_SERVERS",
                System.getProperty("kafka.bootstrap.servers", ""));
        if (bootstrap == null || bootstrap.isBlank()) {
            System.err.println("Set KAFKA_BOOTSTRAP_SERVERS to demo-cluster-1 IAM bootstrap (port 9098).");
            System.err.println("Example: aws kafka get-bootstrap-brokers --cluster-arn <arn> --region us-east-1");
            System.exit(1);
        }

        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", MskProducerProperties.DEFAULT_TOPIC);
        Properties props = MskProducerProperties.forProducer(bootstrap, "surya-test-msk-publisher");

        System.out.printf("Publishing to MSK %s topic=%s IAM=%s%n",
                MskProducerProperties.MSK_CLUSTER_NAME, topic, MskProducerProperties.useMskIam(bootstrap));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String value = args.length > 0 ? String.join(" ", args) : "hello from surya-test-msk-publisher";
            producer.send(new ProducerRecord<>(topic, null, value), (meta, err) -> {
                if (err != null) {
                    err.printStackTrace();
                } else {
                    System.out.printf("Sent to %s partition=%d offset=%d%n",
                            topic, meta.partition(), meta.offset());
                }
            });
            producer.flush();
        }
    }
}
