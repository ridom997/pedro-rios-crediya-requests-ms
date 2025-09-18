package co.com.pedrorido.sqs.listener.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "entrypoint.sqs.listener")
@Data
public class SqsListenerProps {
    private String queueUrl;
    private int maxMessages = 10;
    private int waitTimeSeconds = 20;
    private int visibilityTimeoutSeconds = 60;
    private int requeueVisibilityOnErrorSeconds = 30;
    private int concurrency = 8;
    private Duration idleDelay = Duration.ofMillis(200);
    private Duration handleTimeout = Duration.ofSeconds(30);
    private Duration errorBackoff = Duration.ofSeconds(1);
    private int maxRetries = Integer.MAX_VALUE;
}