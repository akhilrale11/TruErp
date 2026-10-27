package com.example.TradeAI.repository;

import com.example.TradeAI.model.TradingSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingSignalRepository extends JpaRepository<TradingSignal, Long> {
    List<TradingSignal> findTop20ByOrderByGeneratedAtDesc();
    List<TradingSignal> findTop50ByOrderByGeneratedAtDesc();
    List<TradingSignal> findTop10BySymbolOrderByGeneratedAtDesc(String symbol);
}
