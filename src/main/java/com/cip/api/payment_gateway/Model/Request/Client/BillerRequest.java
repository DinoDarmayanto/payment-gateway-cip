package com.cip.api.payment_gateway.Model.Request.Client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillerRequest {

    private String orderId;

    private BigDecimal amount;

    private String paymentMethod;
}