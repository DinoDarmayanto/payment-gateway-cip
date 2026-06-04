package com.cip.api.payment_gateway.Publisher;

import com.cip.api.payment_gateway.Model.Event.TransactionSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.transaction-success}")
    private String transactionSuccessTopic;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    public void publishTransactionSuccess(TransactionSuccessEvent event) {
        if (!kafkaEnabled) {
            log.info(
                    "[KAFKA_PUBLISH_SKIPPED] reason=disabled key={} transactionId={} orderId={}",
                    event.getOrderId(),
                    event.getTransactionId(),
                    event.getOrderId()
            );
            return;
        }

        CompletableFuture.runAsync(() -> doPublish(event))
                .exceptionally(exception -> {
                    log.warn(
                            "[KAFKA_PUBLISH_FAILED] topic={} key={} transactionId={} orderId={} reason={}",
                            transactionSuccessTopic,
                            event.getOrderId(),
                            event.getTransactionId(),
                            event.getOrderId(),
                            exception.getMessage()
                    );
                    return null;
                });
    }

    private void doPublish(TransactionSuccessEvent event) {
        try {
            kafkaTemplate.send(transactionSuccessTopic, event.getOrderId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn(
                                    "[KAFKA_PUBLISH_FAILED] topic={} key={} transactionId={} orderId={} reason={}",
                                    transactionSuccessTopic,
                                    event.getOrderId(),
                                    event.getTransactionId(),
                                    event.getOrderId(),
                                    exception.getMessage()
                            );
                            return;
                        }

                        log.info(
                                "[KAFKA_PUBLISH_SUCCESS] topic={} partition={} offset={} key={} transactionId={} orderId={} status={}",
                                transactionSuccessTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event.getOrderId(),
                                event.getTransactionId(),
                                event.getOrderId(),
                                event.getStatus()
                        );
                    });
        } catch (Exception exception) {
            log.warn(
                    "[KAFKA_PUBLISH_FAILED] topic={} key={} transactionId={} orderId={} reason={}",
                    transactionSuccessTopic,
                    event.getOrderId(),
                    event.getTransactionId(),
                    event.getOrderId(),
                    exception.getMessage()
            );
        }
    }
}
