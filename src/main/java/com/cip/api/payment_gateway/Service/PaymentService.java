package com.cip.api.payment_gateway.Service;

import com.cip.api.payment_gateway.Model.Request.PaymentRequest;
import com.cip.api.payment_gateway.Model.Response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {

    /**
     * Create new payment transaction.
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * Get transaction status by id.
     */
    PaymentResponse getTransaction(UUID transactionId);

}