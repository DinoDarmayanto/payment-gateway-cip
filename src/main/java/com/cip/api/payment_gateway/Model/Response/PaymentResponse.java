package com.cip.api.payment_gateway.Model.Response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID transactionId;

    private String orderId;

    private String status;

    private String corebankReference;

    private String billerReference;

    private String message;
}