package com.cip.api.payment_gateway.Service.impl;

import com.cip.api.payment_gateway.Client.CoreBankClient;
import com.cip.api.payment_gateway.Exception.DuplicateOrderException;
import com.cip.api.payment_gateway.Exception.ExternalServiceException;
import com.cip.api.payment_gateway.Model.Event.TransactionSuccessEvent;
import com.cip.api.payment_gateway.Exception.TransactionNotFoundException;
import com.cip.api.payment_gateway.Model.Entity.Transaction;
import com.cip.api.payment_gateway.Model.Enum.TransactionStatus;
import com.cip.api.payment_gateway.Model.Request.PaymentRequest;
import com.cip.api.payment_gateway.Model.Request.Client.BillerRequest;
import com.cip.api.payment_gateway.Model.Request.Client.CoreBankRequest;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import com.cip.api.payment_gateway.Model.Response.CoreBankResponse;
import com.cip.api.payment_gateway.Model.Response.PaymentResponse;
import com.cip.api.payment_gateway.Publisher.TransactionEventPublisher;
import com.cip.api.payment_gateway.Repository.TransactionRepository;
import com.cip.api.payment_gateway.Service.BillerGatewayService;
import com.cip.api.payment_gateway.Service.PaymentService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String SUCCESS_STATUS = "SUCCESS";

    private final TransactionRepository transactionRepository;
    private final CoreBankClient coreBankClient;
    private final BillerGatewayService billerGatewayService;
    private final TransactionEventPublisher transactionEventPublisher;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("[PAYMENT_CREATE] Start payment | orderId={}", request.getOrderId());

        if (transactionRepository.existsByOrderId(request.getOrderId())) {
            throw new DuplicateOrderException("Order ID already exists");
        }

        Transaction transaction = Transaction.builder()
                .orderId(request.getOrderId())
                .channel(request.getChannel())
                .amount(request.getAmount())
                .account(request.getAccount())
                .currency(request.getCurrency() == null ? "IDR" : request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.PENDING)
                .build();

        transaction = transactionRepository.save(transaction);

        CoreBankResponse coreBankResponse = processCoreBankDebit(transaction);
        if (!isSuccess(coreBankResponse.getStatus())) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setCorebankReference(coreBankResponse.getCorebankReference());
            transaction = transactionRepository.save(transaction);
            return toResponse(transaction, getFailureMessage(coreBankResponse.getMessage(), "Core banking debit failed"));
        }

        transaction.setCorebankReference(coreBankResponse.getCorebankReference());

        BillerResponse billerResponse = processBillerPayment(transaction);
        if (!isSuccess(billerResponse.getStatus())) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setBillerReference(billerResponse.getBillerReference());
            transaction = transactionRepository.save(transaction);
            return toResponse(transaction, getFailureMessage(billerResponse.getMessage(), "Biller payment failed"));
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setBillerReference(billerResponse.getBillerReference());
        transaction = transactionRepository.saveAndFlush(transaction);
        publishTransactionSuccessEventAfterCommit(transaction);

        log.info("[PAYMENT_CREATE] Success payment | id={} | orderId={}",
                transaction.getId(), transaction.getOrderId());

        return toResponse(transaction, null);
    }

    @Override
    public PaymentResponse getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        return toResponse(transaction, null);
    }

    private CoreBankResponse processCoreBankDebit(Transaction transaction) {
        try {
            CoreBankRequest coreBankRequest = CoreBankRequest.builder()
                    .account(transaction.getAccount())
                    .amount(transaction.getAmount())
                    .build();

            return coreBankClient.debit(coreBankRequest);
        } catch (FeignException exception) {
            throw new ExternalServiceException("Failed to call Core Banking service", exception);
        }
    }

    private BillerResponse processBillerPayment(Transaction transaction) {
        BillerRequest billerRequest = BillerRequest.builder()
                .orderId(transaction.getOrderId())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .build();

        return billerGatewayService.pay(billerRequest);
    }

    private boolean isSuccess(String status) {
        return SUCCESS_STATUS.equalsIgnoreCase(status);
    }

    private String getFailureMessage(String externalMessage, String defaultMessage) {
        return externalMessage == null || externalMessage.isBlank() ? defaultMessage : externalMessage;
    }

    private void publishTransactionSuccessEventAfterCommit(Transaction transaction) {
        TransactionSuccessEvent event = TransactionSuccessEvent.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .channel(transaction.getChannel().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .corebankReference(transaction.getCorebankReference())
                .billerReference(transaction.getBillerReference())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    transactionEventPublisher.publishTransactionSuccess(event);
                }
            });
            return;
        }

        transactionEventPublisher.publishTransactionSuccess(event);
    }

    private PaymentResponse toResponse(Transaction transaction, String message) {
        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus().name())
                .corebankReference(transaction.getCorebankReference())
                .billerReference(transaction.getBillerReference())
                .message(message)
                .build();
    }
}
