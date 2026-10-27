package com.example.TradeAI.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portfolio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private Double cashBalance;
    private Double totalInvested;
    private Double totalPortfolioValue;
    private Double realizedPnL;
}
