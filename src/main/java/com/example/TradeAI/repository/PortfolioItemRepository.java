package com.example.TradeAI.repository;

import com.example.TradeAI.model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findByUserId(String userId);
    Optional<PortfolioItem> findByUserIdAndSymbol(String userId, String symbol);
}
