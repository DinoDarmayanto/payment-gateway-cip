package com.cip.api.payment_gateway.Repository;

import com.cip.api.payment_gateway.Model.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);
}