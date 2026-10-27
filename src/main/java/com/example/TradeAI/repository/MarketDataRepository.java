package com.example.TradeAI.repository;

import com.example.TradeAI.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
    List<MarketData> findTop50BySymbolOrderByTimestampDesc(String symbol);
    Optional<MarketData> findTopBySymbolOrderByTimestampDesc(String symbol);
}
