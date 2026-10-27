package com.example.TradeAI.repository;

import com.example.TradeAI.model.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findTop20ByUserIdOrderByExecutedAtDesc(String userId);
}
