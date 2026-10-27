package com.example.TradeAI.service;

import com.example.TradeAI.model.*;
import com.example.TradeAI.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PortfolioService {

    public static final String DEFAULT_USER = "trader_demo";

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MarketDataService marketDataService;

    @Autowired
    public PortfolioService(PortfolioRepository portfolioRepository,
                            PortfolioItemRepository portfolioItemRepository,
                            TradeOrderRepository tradeOrderRepository,
                            WalletTransactionRepository walletTransactionRepository,
                            MarketDataService marketDataService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.marketDataService = marketDataService;
    }

    @PostConstruct
    public void initDemoPortfolio() {
        if (portfolioRepository.findByUserId(DEFAULT_USER).isEmpty()) {
            Portfolio portfolio = Portfolio.builder()
                    .userId(DEFAULT_USER)
                    .cashBalance(100000.00) // Initial paper trading balance $100,000 USD
                    .totalInvested(0.0)
                    .totalPortfolioValue(100000.00)
                    .realizedPnL(0.0)
                    .build();
            portfolioRepository.save(portfolio);
        }
    }

    @Scheduled(fixedRate = 300) // Continuous PnL & Margin Recalculation loop
    public void recalculateLivePositionsPnL() {
        Portfolio portfolio = portfolioRepository.findByUserId(DEFAULT_USER).orElse(null);
        if (portfolio == null) return;

        List<PortfolioItem> items = portfolioItemRepository.findByUserId(DEFAULT_USER);
        double totalUnrealizedPnL = 0.0;
        double invested = 0.0;

        for (PortfolioItem item : items) {
            Double curBid = marketDataService.getCurrentBid(item.getSymbol());
            Double curAsk = marketDataService.getCurrentAsk(item.getSymbol());

            // Buy positions evaluate at current Bid, Sell positions evaluate at current Ask
            double curPrice = item.getQuantity() > 0 ? curBid : curAsk;
            double itemPnL = (curPrice - item.getAverageBuyPrice()) * item.getQuantity();

            item.setCurrentPrice(curPrice);
            item.setUnrealizedPnL(Math.round(itemPnL * 100.0) / 100.0);
            portfolioItemRepository.save(item);

            totalUnrealizedPnL += itemPnL;
            invested += item.getAverageBuyPrice() * Math.abs(item.getQuantity());
        }

        portfolio.setTotalInvested(Math.round(invested * 100.0) / 100.0);
        portfolio.setTotalPortfolioValue(Math.round((portfolio.getCashBalance() + invested + totalUnrealizedPnL) * 100.0) / 100.0);
        portfolioRepository.save(portfolio);
    }

    @Transactional
    public TradeOrder executeOrder(String userId, String symbol, SignalType orderType, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Lot quantity must be greater than zero.");
        }

        Double executionPrice = orderType == SignalType.BUY ? marketDataService.getCurrentAsk(symbol) : marketDataService.getCurrentBid(symbol);
        double totalCost = executionPrice * quantity;

        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseGet(() -> portfolioRepository.save(Portfolio.builder()
                        .userId(userId)
                        .cashBalance(10000.00)
                        .totalInvested(0.0)
                        .totalPortfolioValue(10000.00)
                        .realizedPnL(0.0)
                        .build()));

        if (orderType == SignalType.BUY) {
            if (portfolio.getCashBalance() < totalCost) {
                throw new IllegalStateException("Insufficient margin. Required: $" + totalCost + ", Cash: $" + portfolio.getCashBalance());
            }
            portfolio.setCashBalance(portfolio.getCashBalance() - totalCost);

            Optional<PortfolioItem> existingItemOpt = portfolioItemRepository.findByUserIdAndSymbol(userId, symbol);
            if (existingItemOpt.isPresent()) {
                PortfolioItem item = existingItemOpt.get();
                int newQty = item.getQuantity() + quantity;
                double newAvgPrice = ((item.getQuantity() * item.getAverageBuyPrice()) + totalCost) / newQty;
                item.setQuantity(newQty);
                item.setAverageBuyPrice(Math.round(newAvgPrice * 100.0) / 100.0);
                item.setCurrentPrice(executionPrice);
                item.setUnrealizedPnL(Math.round(((executionPrice - newAvgPrice) * newQty) * 100.0) / 100.0);
                portfolioItemRepository.save(item);
            } else {
                PortfolioItem newItem = PortfolioItem.builder()
                        .userId(userId)
                        .symbol(symbol)
                        .quantity(quantity)
                        .averageBuyPrice(executionPrice)
                        .currentPrice(executionPrice)
                        .unrealizedPnL(0.0)
                        .build();
                portfolioItemRepository.save(newItem);
            }
        } else if (orderType == SignalType.SELL) {
            PortfolioItem item = portfolioItemRepository.findByUserIdAndSymbol(userId, symbol)
                    .orElseThrow(() -> new IllegalStateException("No open position to sell for symbol: " + symbol));

            if (item.getQuantity() < quantity) {
                throw new IllegalStateException("Cannot close more units than held. Holding: " + item.getQuantity());
            }

            double profitLoss = (executionPrice - item.getAverageBuyPrice()) * quantity;
            portfolio.setCashBalance(portfolio.getCashBalance() + (executionPrice * quantity));
            portfolio.setRealizedPnL(portfolio.getRealizedPnL() + profitLoss);

            int remainingQty = item.getQuantity() - quantity;
            if (remainingQty == 0) {
                portfolioItemRepository.delete(item);
            } else {
                item.setQuantity(remainingQty);
                item.setCurrentPrice(executionPrice);
                item.setUnrealizedPnL(Math.round(((executionPrice - item.getAverageBuyPrice()) * remainingQty) * 100.0) / 100.0);
                portfolioItemRepository.save(item);
            }
        }

        recalculateLivePositionsPnL();

        TradeOrder order = TradeOrder.builder()
                .userId(userId)
                .symbol(symbol)
                .orderType(orderType)
                .quantity(quantity)
                .price(executionPrice)
                .totalAmount(totalCost)
                .status("EXECUTED")
                .executedAt(LocalDateTime.now())
                .build();

        return tradeOrderRepository.save(order);
    }

    @Transactional
    public WalletTransaction processWalletTransaction(String userId, String type, String method, Double amount, String referenceId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseGet(() -> portfolioRepository.save(Portfolio.builder().userId(userId).cashBalance(10000.0).build()));

        if ("WITHDRAWAL".equalsIgnoreCase(type)) {
            if (portfolio.getCashBalance() < amount) {
                throw new IllegalStateException("Insufficient cash balance.");
            }
            portfolio.setCashBalance(portfolio.getCashBalance() - amount);
        } else {
            portfolio.setCashBalance(portfolio.getCashBalance() + amount);
        }

        WalletTransaction tx = WalletTransaction.builder()
                .userId(userId)
                .transactionType(type.toUpperCase())
                .paymentMethod(method.toUpperCase())
                .amount(amount)
                .status("COMPLETED")
                .referenceId(referenceId != null && !referenceId.isBlank() ? referenceId : "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .timestamp(LocalDateTime.now())
                .build();

        return walletTransactionRepository.save(tx);
    }

    public List<WalletTransaction> getWalletHistory(String userId) {
        return walletTransactionRepository.findTop20ByUserIdOrderByTimestampDesc(userId);
    }

    public Portfolio getPortfolioSummary(String userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseGet(() -> Portfolio.builder().userId(userId).cashBalance(10000.0).build());
    }

    public List<PortfolioItem> getPortfolioHoldings(String userId) {
        return portfolioItemRepository.findByUserId(userId);
    }

    public List<TradeOrder> getOrderHistory(String userId) {
        return tradeOrderRepository.findTop20ByUserIdOrderByExecutedAtDesc(userId);
    }
}
