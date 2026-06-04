package com.cip.api.payment_gateway.Publisher;

import com.cip.api.payment_gateway.Model.Event.TransactionSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.transaction-success}")
    private String transactionSuccessTopic;

    public void publishTransactionSuccess(TransactionSuccessEvent event) {
        kafkaTemplate.send(transactionSuccessTopic, event.getOrderId(), event);

        log.info(
                "[KAFKA_PUBLISH] topic={} key={} transactionId={} orderId={} status={}",
                transactionSuccessTopic,
                event.getOrderId(),
                event.getTransactionId(),
                event.getOrderId(),
                event.getStatus()
        );
    }
}
