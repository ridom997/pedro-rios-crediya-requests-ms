package co.com.pedrorido.sqs.sender;

import co.com.pedrorido.model.external.RequestCalculateDebtMessage;
import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import co.com.pedrorido.model.external.gateways.MessagePublisherRepository;
import co.com.pedrorido.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender implements MessagePublisherRepository {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper mapper = new ObjectMapper();


    @Override
    public Mono<Void> publishRequestStatusChange(RequestStatusChangeMessage evt) {
        log.info("Sending StatusChange message to SQS {}", evt);
        return serialize(evt)
                .flatMap(json -> Mono.fromFuture(client.sendMessage(
                        buildRequest(properties.requestStatusChangeQueueUrl(), json))))
                .doOnNext(resp -> log.debug("StatusChange sent {}", resp.messageId()))
                .doOnError(e -> log.error("SQS publish error (status change)", e))
                .then();
    }

    @Override
    public Mono<Void> publishCalculateDebtCapacitySqs(RequestCalculateDebtMessage evt) {
        log.info("Sending CalculateDebtCapacity message to SQS {}" , evt);
        return serialize(evt)
                .flatMap(json -> Mono.fromFuture(client.sendMessage(
                        buildRequest(properties.calculateDebtCapacityQueueUrl(), json))))
                .doOnNext(resp -> log.debug("DebtCapacity sent {}", resp.messageId()))
                .doOnError(e -> log.error("SQS publish error (debt capacity)", e))
                .then();
    }

    @Override
    public Mono<Void> publishUpdateCounterQueue(Map<String, String> evt) {
        log.info("Sending UpdateCounter message to SQS {}", evt);
        return serialize(evt)
                .flatMap(json -> Mono.fromFuture(client.sendMessage(
                        buildRequest(properties.updateCounterQueueUrl(), json))))
                .doOnNext(resp -> log.debug("UpdateCounter sent {}", resp.messageId()))
                .doOnError(e -> log.error("SQS publish error (UpdateCounter)", e))
                .then();
    }

    private Mono<String> serialize(Object evt) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(evt));
    }

    private SendMessageRequest buildRequest(String queueUrl, String body) {
        SendMessageRequest.Builder b = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body);
        return b.build();
    }
}
