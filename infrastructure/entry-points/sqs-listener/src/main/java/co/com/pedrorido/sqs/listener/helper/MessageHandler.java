package co.com.pedrorido.sqs.listener.helper;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

public interface MessageHandler {
    Mono<Void> handle(Message msg);
}
