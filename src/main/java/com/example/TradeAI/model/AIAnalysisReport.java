package com.example.TradeAI.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIAnalysisReport {
    private String symbol;
    private String strategyName;
    private Double currentPrice;
    private Double entryPrice;
    private Double targetPrice1;
    private Double targetPrice2;
    private Double stopLossPrice;
    private SignalType recommendedAction;
    private Double confidenceScore;
    private String marketSentiment;
    private String aiRationale;
    private Double rsiValue;
    private String macdSignal;
    private Double sma50;
    private Double sma200;
    private String orderBlockZone;
    private LocalDateTime timestamp;
}
