package com.cip.api.payment_gateway.Service.impl;

import com.cip.api.payment_gateway.Client.BillerClient;
import com.cip.api.payment_gateway.Model.Request.Client.BillerRequest;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import com.cip.api.payment_gateway.Service.BillerGatewayService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillerGatewayServiceImpl implements BillerGatewayService {

    private final BillerClient billerClient;

    @Override
    @CircuitBreaker(name = "billerClient")
    @Retry(name = "billerClient", fallbackMethod = "fallbackPay")
    public BillerResponse pay(BillerRequest request) {
        return billerClient.pay(request);
    }

    @SuppressWarnings("unused")
    private BillerResponse fallbackPay(BillerRequest request, Throwable throwable) {
        log.warn(
                "[BILLER_FALLBACK] orderId={} paymentMethod={} reason={}",
                request.getOrderId(),
                request.getPaymentMethod(),
                throwable.getMessage()
        );

        return BillerResponse.builder()
                .status("FAILED")
                .message("Biller service unavailable")
                .build();
    }
}
