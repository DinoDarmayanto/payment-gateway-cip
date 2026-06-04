package com.cip.api.payment_gateway.Integration;

import com.cip.api.payment_gateway.Client.BillerClient;
import com.cip.api.payment_gateway.Client.CoreBankClient;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import com.cip.api.payment_gateway.Model.Response.CoreBankResponse;
import com.cip.api.payment_gateway.Publisher.TransactionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.kafka.enabled=false",
        "app.security.permit-all=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:payment_gateway_security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoreBankClient coreBankClient;

    @MockBean
    private BillerClient billerClient;

    @MockBean
    private TransactionEventPublisher transactionEventPublisher;

    @BeforeEach
    void setUp() {
        when(coreBankClient.debit(any())).thenReturn(CoreBankResponse.builder()
                .status("SUCCESS")
                .corebankReference("CB123456789")
                .build());

        when(billerClient.pay(any())).thenReturn(BillerResponse.builder()
                .status("SUCCESS")
                .billerReference("BILLER987654321")
                .build());
    }

    @Test
    void paymentApi_shouldReturnUnauthorized_whenNoBearerToken() throws Exception {
        mockMvc.perform(get("/api/payments/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paymentApi_shouldAllowAccess_whenJwtProvided() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("INV-SECURITY-001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orderId").value("INV-SECURITY-001"));
    }

    @Test
    void swaggerDocs_shouldRemainPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void mockCoreBankEndpoint_shouldRemainPublic() throws Exception {
        mockMvc.perform(post("/api/corebank/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account": "1234567890",
                                  "amount": 250000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    private String validRequest(String orderId) {
        return """
                {
                  "orderId": "%s",
                  "channel": "MOBILE_BANKING",
                  "amount": 250000,
                  "account": "1234567890",
                  "currency": "IDR",
                  "paymentMethod": "VIRTUAL_ACCOUNT"
                }
                """.formatted(orderId);
    }
}
