package com.example.TradeAI.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portfolio_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String symbol;
    private Integer quantity;
    private Double averageBuyPrice;
    private Double currentPrice;
    private Double unrealizedPnL;
}
