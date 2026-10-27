package com.example.TradeAI.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String transactionType; // DEPOSIT or WITHDRAWAL
    private String paymentMethod;   // UPI, USDT_TRC20, CRYPTO_BTC, BANK_WIRE
    private Double amount;
    private String status;          // COMPLETED, PENDING
    private String referenceId;     // Transaction Hash / UTR Number
    private LocalDateTime timestamp;
}
