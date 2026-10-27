package com.example.TradeAI.repository;

import com.example.TradeAI.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findTop20ByUserIdOrderByTimestampDesc(String userId);
}
