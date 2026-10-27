package com.example.TradeAI.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_signals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String timeframe;
    private String strategyName;

    @Enumerated(EnumType.STRING)
    private SignalType signalType;

    private String status; // ANALYZING, SETUP_DETECTED, WAITING_FOR_CONFIRMATION, ACTIVE, EXPIRED, INVALIDATED

    private Double confidenceScore;
    private String marketCondition; // TRENDING_UP, TRENDING_DOWN, RANGING, HIGH_VOLATILITY, CONSOLIDATION

    private Double currentPrice;
    private Double bidPrice;
    private Double askPrice;
    private Double spread;

    private Double entryPrice;
    private Double entryZoneMin;
    private Double entryZoneMax;

    private Double stopLossPrice;
    private Double takeProfit1;
    private Double takeProfit2;
    private Double takeProfit3;
    private Double riskRewardRatio;

    @Column(length = 2000)
    private String aiRationale;

    @Column(length = 2000)
    private String reasonsListJson; // JSON array of bullet points

    @Column(length = 1000)
    private String technicalIndicatorsSummary;

    private Integer validForMinutes;
    private LocalDateTime validUntil;
    private LocalDateTime generatedAt;
}
