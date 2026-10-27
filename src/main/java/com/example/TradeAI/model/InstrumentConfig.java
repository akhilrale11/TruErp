package com.example.TradeAI.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentConfig {
    private String symbol;
    private String name;
    private String type; // FOREX, METALS, INDICES, CRYPTO
    private Double basePrice;
    private Double tickSize;
    private Integer decimalPlaces;
    private Double spread;
    private Double volatility;
    private String tradingHours;
}
