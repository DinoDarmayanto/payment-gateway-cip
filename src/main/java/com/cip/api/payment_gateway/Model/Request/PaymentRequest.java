package com.cip.api.payment_gateway.Model.Request;

import com.cip.api.payment_gateway.Model.Enum.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank
    private String orderId;

    @NotNull
    private Channel channel;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String account;

    @Builder.Default
    @NotBlank
    private String currency = "IDR";

    @NotBlank
    private String paymentMethod;
}
