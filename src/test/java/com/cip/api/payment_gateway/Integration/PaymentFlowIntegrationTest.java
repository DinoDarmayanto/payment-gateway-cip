package com.cip.api.payment_gateway.Integration;

import com.cip.api.payment_gateway.Client.BillerClient;
import com.cip.api.payment_gateway.Client.CoreBankClient;
import com.cip.api.payment_gateway.Publisher.TransactionEventPublisher;
import com.cip.api.payment_gateway.Repository.TransactionRepository;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import com.cip.api.payment_gateway.Model.Response.CoreBankResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.kafka.enabled=false",
        "app.security.permit-all=true",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:payment_gateway_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockBean
    private CoreBankClient coreBankClient;

    @MockBean
    private BillerClient billerClient;

    @MockBean
    private TransactionEventPublisher transactionEventPublisher;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void createPayment_shouldReturnSuccess_whenFlowSucceeds() throws Exception {
        when(coreBankClient.debit(any())).thenReturn(CoreBankResponse.builder()
                .status("SUCCESS")
                .corebankReference("CB123456789")
                .build());

        when(billerClient.pay(any())).thenReturn(BillerResponse.builder()
                .status("SUCCESS")
                .billerReference("BILLER987654321")
                .build());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("INV-SUCCESS-001", "VIRTUAL_ACCOUNT", 250000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("INV-SUCCESS-001"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.corebankReference").value("CB123456789"))
                .andExpect(jsonPath("$.billerReference").value("BILLER987654321"));

        verify(transactionEventPublisher, times(1)).publishTransactionSuccess(any());
    }

    @Test
    void createPayment_shouldReturnConflict_whenOrderIdAlreadyExists() throws Exception {
        when(coreBankClient.debit(any())).thenReturn(CoreBankResponse.builder()
                .status("SUCCESS")
                .corebankReference("CB123456789")
                .build());

        when(billerClient.pay(any())).thenReturn(BillerResponse.builder()
                .status("SUCCESS")
                .billerReference("BILLER987654321")
                .build());

        String duplicateOrderId = "INV-DUPLICATE-001";

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(duplicateOrderId, "VIRTUAL_ACCOUNT", 250000)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(duplicateOrderId, "VIRTUAL_ACCOUNT", 250000)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Order ID already exists"));
    }

    @Test
    void createPayment_shouldReturnFailed_whenCoreBankFailed() throws Exception {
        when(coreBankClient.debit(any())).thenReturn(CoreBankResponse.builder()
                .status("FAILED")
                .message("Insufficient balance")
                .build());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("INV-COREBANK-FAILED-001", "VIRTUAL_ACCOUNT", 2000000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Insufficient balance"));

        verify(billerClient, never()).pay(any());
        verify(transactionEventPublisher, never()).publishTransactionSuccess(any());
    }

    @Test
    void createPayment_shouldReturnFailed_whenBillerFailed() throws Exception {
        when(coreBankClient.debit(any())).thenReturn(CoreBankResponse.builder()
                .status("SUCCESS")
                .corebankReference("CB123456789")
                .build());

        when(billerClient.pay(any())).thenReturn(BillerResponse.builder()
                .status("FAILED")
                .message("Biller payment failed")
                .build());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("INV-BILLER-FAILED-001", "FAILED_METHOD", 250000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Biller payment failed"));

        verify(transactionEventPublisher, never()).publishTransactionSuccess(any());
    }

    private String validRequest(String orderId, String paymentMethod, int amount) {
        return """
                {
                  "orderId": "%s",
                  "channel": "MOBILE_BANKING",
                  "amount": %d,
                  "account": "1234567890",
                  "currency": "IDR",
                  "paymentMethod": "%s"
                }
                """.formatted(orderId, amount, paymentMethod);
    }
}
