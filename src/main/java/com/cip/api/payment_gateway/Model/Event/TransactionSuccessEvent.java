package com.cip.api.payment_gateway.Model.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSuccessEvent {

    private UUID transactionId;

    private String orderId;

    private String channel;

    private BigDecimal amount;

    private String currency;

    private String paymentMethod;

    private String corebankReference;

    private String billerReference;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime updatedAt;
}
