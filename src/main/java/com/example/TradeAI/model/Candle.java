package com.example.TradeAI.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candle {
    private long time; // Unix timestamp in seconds
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
}
