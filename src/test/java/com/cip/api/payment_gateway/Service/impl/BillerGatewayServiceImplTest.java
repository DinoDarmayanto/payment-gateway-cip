package com.cip.api.payment_gateway.Service.impl;

import com.cip.api.payment_gateway.Client.BillerClient;
import com.cip.api.payment_gateway.Model.Request.Client.BillerRequest;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import com.cip.api.payment_gateway.Service.BillerGatewayService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "app.kafka.enabled=false",
        "app.security.permit-all=true",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:payment_gateway_biller_gateway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class BillerGatewayServiceImplTest {

    @Autowired
    private BillerGatewayService billerGatewayService;

    @MockBean
    private BillerClient billerClient;

    @Test
    void pay_shouldReturnFailedResponse_whenBillerIsUnavailable() {
        when(billerClient.pay(any(BillerRequest.class)))
                .thenThrow(serviceUnavailableException());

        BillerResponse response = billerGatewayService.pay(BillerRequest.builder()
                .orderId("INV-FALLBACK-001")
                .amount(BigDecimal.valueOf(250000))
                .paymentMethod("VIRTUAL_ACCOUNT")
                .build());

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertEquals("Biller service unavailable", response.getMessage());

        verify(billerClient, times(3)).pay(any(BillerRequest.class));
    }

    private FeignException serviceUnavailableException() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/api/biller/pay",
                Collections.emptyMap(),
                new byte[0],
                StandardCharsets.UTF_8,
                null
        );

        Response response = Response.builder()
                .request(request)
                .status(503)
                .reason("Service Unavailable")
                .headers(Collections.emptyMap())
                .build();

        return FeignException.errorStatus("BillerClient#pay", response);
    }
}
