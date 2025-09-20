package co.com.pedrorido.sqs.listener;

import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import co.com.pedrorido.usecase.apis.IRequestEventsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class SQSProcessor implements Function<Message, Mono<Void>> {
    private final IRequestEventsApi myUseCase;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> apply(Message message) {
        RequestStatusChangeMessage requestStatusChangeMessage = null;
        try {
            log.info("Processing message {}", message.body());
            requestStatusChangeMessage = objectMapper.readValue(message.body(), RequestStatusChangeMessage.class);
        } catch (Exception e) {
            log.error("Error processing message {}", message.body(), e);
            // Opcional: podrías ACKear para no ciclar si es definitivamente ilegible
            return Mono.error(e);
        }

        return myUseCase.onStatusChange(requestStatusChangeMessage);
    }
}
