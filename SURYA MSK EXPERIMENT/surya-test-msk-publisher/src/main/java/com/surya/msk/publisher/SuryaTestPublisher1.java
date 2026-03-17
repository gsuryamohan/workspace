package com.surya.msk.publisher;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Surya-Test-Publisher-1: infinite loop — at least <strong>one</strong> message per second, each value
 * random alphanumeric text of <strong>1,024 characters</strong> (~1 KB), to MSK <strong>demo-cluster-1</strong>
 * / topic <strong>surya-test-topic</strong> (defaults).
 * <p>
 * Optional env: {@code PUBLISHER_PAYLOAD_CHARS} (default 1024), {@code PUBLISHER_INTERVAL_MS} (default 1000).
 * <p>
 * Required: {@code KAFKA_BOOTSTRAP_SERVERS} — IAM bootstrap (port {@code 9098}). Stop with Ctrl+C / SIGTERM.
 */
public final class SuryaTestPublisher1 {

    /** ~1 KB UTF-16 text per message */
    public static final int DEFAULT_PAYLOAD_CHAR_COUNT = 1024;
    public static final int DEFAULT_INTERVAL_MS = 1000;

    private static final String ALPHANUM =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static void main(String[] args) {
        String bootstrap = System.getenv().getOrDefault(
                "KAFKA_BOOTSTRAP_SERVERS",
                System.getProperty("kafka.bootstrap.servers", ""));
        if (bootstrap == null || bootstrap.isBlank()) {
            System.err.println("Set KAFKA_BOOTSTRAP_SERVERS to demo-cluster-1 IAM bootstrap (port 9098).");
            System.exit(1);
        }

        String topic = System.getenv().getOrDefault("KAFKA_TOPIC", MskProducerProperties.DEFAULT_TOPIC);
        int payloadChars = parseIntEnv("PUBLISHER_PAYLOAD_CHARS", DEFAULT_PAYLOAD_CHAR_COUNT, 1, 1_000_000);
        int intervalMs = parseIntEnv("PUBLISHER_INTERVAL_MS", DEFAULT_INTERVAL_MS, 100, 3_600_000);

        Properties base = MskProducerProperties.forProducer(bootstrap, "Surya-Test-Publisher-1");
        base.put(ProducerConfig.LINGER_MS_CONFIG, Math.min(50, intervalMs / 2));

        KafkaProducer<String, String> producer = new KafkaProducer<>(base);
        AtomicLong seq = new AtomicLong(0);
        boolean iam = MskProducerProperties.useMskIam(bootstrap);
        final int chars = payloadChars;

        Runnable publish = () -> {
            try {
                String payload = randomPayload(chars);
                long n = seq.incrementAndGet();
                String key = "msg-" + n;
                producer.send(
                        new ProducerRecord<>(topic, key, payload),
                        (meta, err) -> {
                            if (err != null) {
                                System.err.printf("[%tT] #%d send failed: %s%n", System.currentTimeMillis(), n, err.getMessage());
                                err.printStackTrace();
                            } else {
                                System.out.printf("[%tT] #%d p=%d off=%d len=%d%n",
                                        System.currentTimeMillis(), n,
                                        meta.partition(), meta.offset(), payload.length());
                            }
                        });
            } catch (Exception e) {
                System.err.printf("[%tT] publish error: %s%n", System.currentTimeMillis(), e.getMessage());
                e.printStackTrace();
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down producer...");
            producer.close(Duration.ofSeconds(30));
        }));

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Surya-Test-Publisher-1-scheduler");
            t.setDaemon(false);
            return t;
        });

        System.out.printf("Surya-Test-Publisher-1 → MSK %s topic=%s IAM=%s every %dms payload=%d chars (infinite; Ctrl+C stop)%n",
                MskProducerProperties.MSK_CLUSTER_NAME, topic, iam, intervalMs, chars);
        scheduler.scheduleAtFixedRate(publish, 0, intervalMs, TimeUnit.MILLISECONDS);

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scheduler.shutdownNow();
            producer.close(Duration.ofSeconds(30));
        }
    }

    private static int parseIntEnv(String name, int def, int min, int max) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) return def;
        try {
            int n = Integer.parseInt(v.trim());
            return Math.max(min, Math.min(max, n));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    static String randomPayload(int charCount) {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(charCount);
        for (int i = 0; i < charCount; i++) {
            sb.append(ALPHANUM.charAt(r.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
