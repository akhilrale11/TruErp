package com.example.TradeAI.controller;

import com.example.TradeAI.model.*;
import com.example.TradeAI.service.PortfolioService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Autowired
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public Portfolio getPortfolioSummary() {
        return portfolioService.getPortfolioSummary(PortfolioService.DEFAULT_USER);
    }

    @GetMapping("/holdings")
    public List<PortfolioItem> getHoldings() {
        return portfolioService.getPortfolioHoldings(PortfolioService.DEFAULT_USER);
    }

    @GetMapping("/orders")
    public List<TradeOrder> getOrderHistory() {
        return portfolioService.getOrderHistory(PortfolioService.DEFAULT_USER);
    }

    @PostMapping("/orders")
    public TradeOrder placeOrder(@RequestBody OrderRequest request) {
        return portfolioService.executeOrder(
                PortfolioService.DEFAULT_USER,
                request.getSymbol(),
                request.getOrderType(),
                request.getQuantity()
        );
    }

    @GetMapping("/wallet/history")
    public List<WalletTransaction> getWalletHistory() {
        return portfolioService.getWalletHistory(PortfolioService.DEFAULT_USER);
    }

    @PostMapping("/wallet/transact")
    public WalletTransaction transactWallet(@RequestBody WalletRequest request) {
        return portfolioService.processWalletTransaction(
                PortfolioService.DEFAULT_USER,
                request.getTransactionType(),
                request.getPaymentMethod(),
                request.getAmount(),
                request.getReferenceId()
        );
    }

    @Data
    public static class OrderRequest {
        private String symbol;
        private SignalType orderType;
        private int quantity;
    }

    @Data
    public static class WalletRequest {
        private String transactionType; // DEPOSIT or WITHDRAWAL
        private String paymentMethod;   // UPI, USDT_TRC20, CRYPTO_BTC, BANK_WIRE
        private Double amount;
        private String referenceId;
    }
}
