package com.cip.api.payment_gateway.Service.impl;

import com.cip.api.payment_gateway.Client.CoreBankClient;
import com.cip.api.payment_gateway.Exception.DuplicateOrderException;
import com.cip.api.payment_gateway.Exception.TransactionNotFoundException;
import com.cip.api.payment_gateway.Model.Entity.Transaction;
import com.cip.api.payment_gateway.Model.Enum.Channel;
import com.cip.api.payment_gateway.Model.Enum.TransactionStatus;
import com.cip.api.payment_gateway.Model.Request.Client.BillerRequest;
import com.cip.api.payment_gateway.Model.Request.Client.CoreBankRequest;
import com.cip.api.payment_gateway.Model.Request.PaymentRequest;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import com.cip.api.payment_gateway.Model.Response.CoreBankResponse;
import com.cip.api.payment_gateway.Model.Response.PaymentResponse;
import com.cip.api.payment_gateway.Publisher.TransactionEventPublisher;
import com.cip.api.payment_gateway.Repository.TransactionRepository;
import com.cip.api.payment_gateway.Service.BillerGatewayService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CoreBankClient coreBankClient;

    @Mock
    private BillerGatewayService billerGatewayService;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createPayment_shouldReturnSuccess_whenCoreBankAndBillerSuccess() {
        PaymentRequest request = buildPaymentRequest();

        Transaction pendingTransaction = buildTransaction(TransactionStatus.PENDING);
        Transaction successTransaction = buildTransaction(TransactionStatus.SUCCESS);
        successTransaction.setCorebankReference("CB123456789");
        successTransaction.setBillerReference("BILLER987654321");

        when(transactionRepository.existsByOrderId("INV-TEST-001")).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(pendingTransaction)
                .thenReturn(successTransaction);

        when(coreBankClient.debit(any(CoreBankRequest.class)))
                .thenReturn(CoreBankResponse.builder()
                        .corebankReference("CB123456789")
                        .status("SUCCESS")
                        .build());

        when(billerGatewayService.pay(any(BillerRequest.class)))
                .thenReturn(BillerResponse.builder()
                        .billerReference("BILLER987654321")
                        .status("SUCCESS")
                        .build());

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals("INV-TEST-001", response.getOrderId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("CB123456789", response.getCorebankReference());
        assertEquals("BILLER987654321", response.getBillerReference());
        assertNull(response.getMessage());

        verify(coreBankClient, times(1)).debit(any(CoreBankRequest.class));
        verify(billerGatewayService, times(1)).pay(any(BillerRequest.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(transactionEventPublisher, times(1)).publishTransactionSuccess(any());
    }

    @Test
    void createPayment_shouldThrowDuplicateOrderException_whenOrderIdAlreadyExists() {
        PaymentRequest request = buildPaymentRequest();

        when(transactionRepository.existsByOrderId("INV-TEST-001")).thenReturn(true);

        assertThrows(DuplicateOrderException.class, () -> paymentService.createPayment(request));

        verify(coreBankClient, never()).debit(any());
        verify(billerGatewayService, never()).pay(any());
        verify(transactionRepository, never()).save(any());
        verify(transactionEventPublisher, never()).publishTransactionSuccess(any());
    }

    @Test
    void createPayment_shouldReturnFailed_whenCoreBankFailed() {
        PaymentRequest request = buildPaymentRequest();

        Transaction pendingTransaction = buildTransaction(TransactionStatus.PENDING);
        Transaction failedTransaction = buildTransaction(TransactionStatus.FAILED);

        when(transactionRepository.existsByOrderId("INV-TEST-001")).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(pendingTransaction)
                .thenReturn(failedTransaction);

        when(coreBankClient.debit(any(CoreBankRequest.class)))
                .thenReturn(CoreBankResponse.builder()
                        .status("FAILED")
                        .message("Insufficient balance")
                        .build());

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertEquals("Insufficient balance", response.getMessage());

        verify(coreBankClient, times(1)).debit(any(CoreBankRequest.class));
        verify(billerGatewayService, never()).pay(any());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(transactionEventPublisher, never()).publishTransactionSuccess(any());
    }

    @Test
    void createPayment_shouldReturnFailed_whenBillerFailed() {
        PaymentRequest request = buildPaymentRequest();

        Transaction pendingTransaction = buildTransaction(TransactionStatus.PENDING);
        Transaction failedTransaction = buildTransaction(TransactionStatus.FAILED);
        failedTransaction.setCorebankReference("CB123456789");

        when(transactionRepository.existsByOrderId("INV-TEST-001")).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(pendingTransaction)
                .thenReturn(failedTransaction);

        when(coreBankClient.debit(any(CoreBankRequest.class)))
                .thenReturn(CoreBankResponse.builder()
                        .corebankReference("CB123456789")
                        .status("SUCCESS")
                        .build());

        when(billerGatewayService.pay(any(BillerRequest.class)))
                .thenReturn(BillerResponse.builder()
                        .status("FAILED")
                        .message("Biller payment failed")
                        .build());

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        assertEquals("Biller payment failed", response.getMessage());

        verify(coreBankClient, times(1)).debit(any(CoreBankRequest.class));
        verify(billerGatewayService, times(1)).pay(any(BillerRequest.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(transactionEventPublisher, never()).publishTransactionSuccess(any());
    }

    @Test
    void getTransaction_shouldReturnTransaction_whenTransactionExists() {
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = buildTransaction(TransactionStatus.SUCCESS);
        transaction.setId(transactionId);
        transaction.setCorebankReference("CB123456789");
        transaction.setBillerReference("BILLER987654321");

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        PaymentResponse response = paymentService.getTransaction(transactionId);

        assertNotNull(response);
        assertEquals(transactionId, response.getTransactionId());
        assertEquals("INV-TEST-001", response.getOrderId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("CB123456789", response.getCorebankReference());
        assertEquals("BILLER987654321", response.getBillerReference());

        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void getTransaction_shouldThrowTransactionNotFoundException_whenTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> paymentService.getTransaction(transactionId));

        verify(transactionRepository, times(1)).findById(transactionId);
    }

    private PaymentRequest buildPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId("INV-TEST-001");
        request.setChannel(Channel.MOBILE_BANKING);
        request.setAmount(BigDecimal.valueOf(250000));
        request.setAccount("1234567890");
        request.setCurrency("IDR");
        request.setPaymentMethod("VIRTUAL_ACCOUNT");
        return request;
    }

    private Transaction buildTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .orderId("INV-TEST-001")
                .channel(Channel.MOBILE_BANKING)
                .amount(BigDecimal.valueOf(250000))
                .account("1234567890")
                .currency("IDR")
                .paymentMethod("VIRTUAL_ACCOUNT")
                .status(status)
                .build();
    }
}
