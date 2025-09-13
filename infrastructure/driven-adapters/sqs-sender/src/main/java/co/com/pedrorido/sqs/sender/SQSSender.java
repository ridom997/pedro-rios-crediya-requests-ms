package co.com.pedrorido.sqs.sender;

import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import co.com.pedrorido.model.external.gateways.MessagePublisherRepository;
import co.com.pedrorido.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender implements MessagePublisherRepository {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    @Value("${messaging.sqs.estadoQueueUrl}") String queueUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.debug("Message sent {}", response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }

    @Override
    public Mono<Void> publishRequestStatusChange(RequestStatusChangeMessage evt) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(evt))
                .flatMap(json -> Mono.fromFuture(
                        client.sendMessage(SendMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .messageBody(json)
                                // FIFO
                                .messageGroupId("solicitud-" + evt.solicitudId())
                                .messageDeduplicationId(evt.solicitudId())
                                .build())))
                .then();
    }
}
