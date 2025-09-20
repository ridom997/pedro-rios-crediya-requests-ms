package co.com.pedrorido.sqs.sender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs")
public record SQSSenderProperties(
     String region,
     String requestStatusChangeQueueUrl,
     String calculateDebtCapacityQueueUrl,
     String endpoint){
}
